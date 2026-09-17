#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
APP_PROPS_FILE="${SCRIPT_DIR}/src/main/resources/application.properties"
APP_PROPS_MODEL_FILE="${SCRIPT_DIR}/src/main/resources/application.properties.model"

usage() {
  cat <<USAGE
Usage: ./deploy.sh <command> [options]

Commands:
  install                    Build images and start services
  update                     Update source with Git, rebuild without cache, and recreate services
  rebuild                    Rebuild images with current code and recreate services
  restart                    Restart existing containers
  start                      Start existing containers
  stop                       Stop containers without removing volumes or data
  logs [args...]             Show logs
  cleanup                    Delete all exported files in data/
  health                     Check Bulk Downloader and Solr endpoints
  shell                      Open a shell in the Bulk Downloader container
  help                       Show this help

Expected docker compose services:
  - bulk-downloader
USAGE
}

ensure_compose_file_exists() {
  if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "Error: ${COMPOSE_FILE} was not found." >&2
    echo "Create docker-compose.yml with service bulk-downloader before running this script." >&2
    exit 1
  fi
}

ensure_docker_installed() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "Error: docker is not available in PATH." >&2
    exit 1
  fi

  if ! docker compose version >/dev/null 2>&1; then
    echo "Error: docker compose plugin v2 is required." >&2
    exit 1
  fi
}

update_source_checkout() {
  if ! command -v git >/dev/null 2>&1; then
    echo "Error: git is not available in PATH; cannot update the source checkout." >&2
    exit 1
  fi

  if ! git -C "${SCRIPT_DIR}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "Error: ${SCRIPT_DIR} is not a Git checkout; cannot update the source code." >&2
    exit 1
  fi

  local changes
  changes="$(git -C "${SCRIPT_DIR}" status --porcelain=v1)"
  if [ -n "${changes}" ]; then
    echo "Error: local changes found in ${SCRIPT_DIR}; commit, stash, or remove them before update." >&2
    printf "%s\n" "${changes}" >&2
    exit 1
  fi

  echo "Updating source code..."
  git -C "${SCRIPT_DIR}" pull --ff-only
}

get_app_property() {
  local key="$1"
  local value=""

  if [ ! -f "${APP_PROPS_FILE}" ]; then
    printf ""
    return 0
  fi

  value="$(awk -v k="${key}" '
    {
      line = $0
      sub(/\r$/, "", line)
      if (line ~ /^[[:space:]]*(#|$)/) {
        next
      }

      eq = index(line, "=")
      if (eq == 0) {
        next
      }

      name = substr(line, 1, eq - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name != k) {
        next
      }

      v = substr(line, eq + 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", v)
      print v
      exit
    }
  ' "${APP_PROPS_FILE}" || true)"

  printf "%s" "${value}"
}


ensure_application_properties() {
  if [ ! -f "${APP_PROPS_FILE}" ]; then
    echo "Error: ${APP_PROPS_FILE} was not found." >&2
    if [ -f "${APP_PROPS_MODEL_FILE}" ]; then
      echo "Create it manually by copying ${APP_PROPS_MODEL_FILE}, then edit its values." >&2
    else
      echo "Create it manually and configure the application properties before running this script." >&2
    fi
    exit 1
  fi
}

export_runtime_env() {
  local project_name
  local server_port

  project_name="${COMPOSE_PROJECT_NAME:-vufind-bulk-downloader}"
  server_port="$(get_app_property "server.port")"
  if [ -z "${server_port}" ]; then
    echo "Error: server.port was not found in ${APP_PROPS_FILE}." >&2
    exit 1
  fi

  export COMPOSE_PROJECT_NAME="${project_name}"
  export SERVER_PORT="${server_port}"
}

dc() {
  export_runtime_env
  ensure_compose_file_exists
  docker compose -f "${COMPOSE_FILE}" "$@"
}

build_images() {
  local no_cache="${1:-false}"
  local args

  args=(build)
  if [ "${no_cache}" = true ]; then
    args+=(--no-cache)
  fi

  dc "${args[@]}" bulk-downloader
}

prepare_data_directory() {
  local data_dir
  local container_owner

  data_dir="${SCRIPT_DIR}/data"
  mkdir -p "${data_dir}"

  container_owner="$(dc run --rm --no-deps --entrypoint sh bulk-downloader -c 'printf "%s:%s" "$(id -u)" "$(id -g)"')"
  if [[ ! "${container_owner}" =~ ^[0-9]+:[0-9]+$ ]]; then
    echo "Error: could not determine the UID:GID of the bulk-downloader container user." >&2
    exit 1
  fi

  echo "Granting ${container_owner} write access to ${data_dir}..."
  if [ "$(id -u)" -eq 0 ]; then
    chown -R "${container_owner}" "${data_dir}"
    chmod -R u+rwX "${data_dir}"
  else
    sudo chown -R "${container_owner}" "${data_dir}"
    sudo chmod -R u+rwX "${data_dir}"
  fi
}
cleanup_exports() {
  local data_dir

  data_dir="${SCRIPT_DIR}/data"
  if [ ! -d "${data_dir}" ]; then
    echo "No export directory found at ${data_dir}; nothing to clean."
    return
  fi

  echo "Deleting all exported files from ${data_dir}..."
  find "${data_dir}" -type f -print -delete
}

start_environment() {
  prepare_data_directory
  dc up -d bulk-downloader
  echo "Bulk downloader: http://localhost:${SERVER_PORT}"
}

recreate_environment() {
  prepare_data_directory
  dc up -d --force-recreate bulk-downloader
  echo "Bulk downloader: http://localhost:${SERVER_PORT}"
}

cmd="${1:-help}"
if [ "$#" -gt 0 ]; then
  shift
fi

case "${cmd}" in
  help|-h|--help)
    usage
    ;;

  install)
    ensure_application_properties
    ensure_docker_installed
    build_images false
    start_environment
    ;;

  update)
    ensure_application_properties
    ensure_docker_installed
    update_source_checkout
    build_images true
    recreate_environment
    ;;

  rebuild)
    ensure_application_properties
    ensure_docker_installed
    build_images false
    recreate_environment
    ;;

  restart)
    ensure_application_properties
    ensure_docker_installed
    prepare_data_directory
    dc restart bulk-downloader
    ;;

  start)
    ensure_application_properties
    ensure_docker_installed
    prepare_data_directory
    dc start bulk-downloader
    ;;

  stop)
    ensure_application_properties
    ensure_docker_installed
    dc stop bulk-downloader
    ;;

  cleanup)
    cleanup_exports
    ;;

  logs)
    ensure_application_properties
    ensure_docker_installed
    if [ "$#" -eq 0 ]; then
      dc logs -f bulk-downloader
    else
      dc logs "$@"
    fi
    ;;

  health)
    ensure_application_properties
    ensure_docker_installed
    solr_server_url=""
    solr_server_url="$(get_app_property "solr.server.url")"

    dc ps
    echo
    curl -fsS -o /dev/null -w "Bulk:   http://localhost:${SERVER_PORT} -> %{http_code}\n" "http://localhost:${SERVER_PORT}/" || true
    if [ -n "${solr_server_url}" ]; then
      curl -fsS -o /dev/null -w "Solr:   ${solr_server_url}/select?q=*:*&rows=0 -> %{http_code}\n" "${solr_server_url}/select?q=*:*&rows=0" || true
    else
      echo "Solr:   solr.server.url not found in src/main/resources/application.properties"
    fi
    ;;

  shell)
    ensure_application_properties
    ensure_docker_installed
    dc exec bulk-downloader sh
    ;;

  *)
    usage
    exit 1
    ;;
esac
