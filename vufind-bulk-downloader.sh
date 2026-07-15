#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
ENV_FILE="${SCRIPT_DIR}/.env"
ENV_EXAMPLE="${SCRIPT_DIR}/.env.example"
APP_PROPS_FILE="${SCRIPT_DIR}/src/main/resources/application.properties"

usage() {
  cat <<USAGE
Usage: ./vufind-bulk-downloader.sh <command> [options]

Commands:
  generate-config            Generate src/main/resources/application.properties from .env
  install                    Build images and start services
  update                     Rebuild without cache and recreate services
  rebuild                    Rebuild images with current code and recreate services
  restart                    Restart existing containers
  start                      Start existing containers
  stop                       Stop containers without removing volumes or data
  logs [args...]             Show logs
  health                     Check Bulk Downloader and Solr endpoints
  shell                      Open a shell in the Bulk Downloader container
  help                       Show this help

Expected docker compose services:
  - bulk-downloader
USAGE
}

ensure_env_file() {
  if [ ! -f "${ENV_FILE}" ]; then
    if [ -f "${ENV_EXAMPLE}" ]; then
      cp "${ENV_EXAMPLE}" "${ENV_FILE}"
    else
      touch "${ENV_FILE}"
    fi
  fi
}

ensure_compose_file_exists() {
  if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "Error: ${COMPOSE_FILE} was not found." >&2
    echo "Create docker-compose.yml with service bulk-downloader before running this script." >&2
    exit 1
  fi
}

get_env_var() {
  local key="$1"
  local default_value="$2"
  local value=""

  if [ -f "${ENV_FILE}" ]; then
    local found
    found="$(awk -v k="${key}" '
      {
        line = $0
        sub(/\r$/, "", line)
        if (NR == 1) {
          sub(/^\357\273\277/, "", line)
        }
        sub(/^[[:space:]]*export[[:space:]]+/, "", line)
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
        if ((v ~ /^".*"$/) || (v ~ /^'\''.*'\''$/)) {
          v = substr(v, 2, length(v) - 2)
        }
      }
      END {
        print v
      }
    ' "${ENV_FILE}" || true)"
    if [ -n "${found}" ]; then
      value="${found}"
    fi
  fi

  if [ -z "${value}" ]; then
    value="${default_value}"
  fi

  printf "%s\n" "${value}"
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

generate_application_properties() {
  ensure_env_file
  mkdir -p "$(dirname "${APP_PROPS_FILE}")"

  cat > "${APP_PROPS_FILE}" <<EOF
##Solr query properties##

#Server address
solr.server.url = $(get_env_var APP_SOLR_SERVER_URL "http://localhost:8983/solr/biblio")

##CSV file properties##

#Directory where files will be saved to
file.path=$(get_env_var APP_FILE_PATH "/app/data/")
#Separator character
file.sep-char = $(get_env_var APP_FILE_SEP_CHAR ",")
#Symbol used to separate values in multiple-value fields
file.list-sep = $(get_env_var APP_FILE_LIST_SEP "||")
#CSV column headers. This list of fields must be synchronized with the list of
#fields available for export defined in VuFind. If fields are to be aggregated
#in a single column, the header must be set to "null", and the new column header
#defined in the 'file.agg-fields' property below
file.header = $(get_env_var APP_FILE_HEADER '{"title":"T\u00edtulo do documento","publishDate":"Ano de publica\u00e7\u00e3o","author":"Autores","instname_str":"T\u00edtulo da institui\u00e7\u00e3o fonte","institution":"Sigla da institui\u00e7\u00e3o fonte","format":"Tipo de documento","dc.identifier.citation.fl_str_mv":"Refer\u00eancia"}')

#Aggregated fields, which gather multiple fields in a single column as a list
file.agg-fields = $(get_env_var APP_FILE_AGG_FIELDS '{"Orientadores":"dc.contributor.advisor1.fl_str_mv,dc.contributor.advisor2.fl_str_mv","ID Lattes dos orientadores":"dc.contributor.advisor1Lattes.fl_str_mv,dc.contributor.advisor2Lattes.fl_str_mv","Membros da banca":"dc.contributor.referee1.fl_str_mv,dc.contributor.referee2.fl_str_mv,dc.contributor.referee3.fl_str_mv,dc.contributor.referee4.fl_str_mv,dc.contributor.referee5.fl_str_mv","ID Lattes dos membros da banca":"dc.contributor.referee1Lattes.fl_str_mv,dc.contributor.referee2Lattes.fl_str_mv,dc.contributor.referee3Lattes.fl_str_mv,dc.contributor.referee4Lattes.fl_str_mv,dc.contributor.referee5Lattes.fl_str_mv"}')

#Message to be show for empty fields
file.null-msg = $(get_env_var APP_FILE_NULL_MSG "N\u00e3o informado pela institui\u00e7\u00e3o")
#Fields for which the message should not be shown if empty
file.no-msg-fields = $(get_env_var APP_FILE_NO_MSG_FIELDS "dc.contributor.co.fl_str_mv")

##Email properties##
spring.mail.host=$(get_env_var APP_MAIL_HOST "smtp.gmail.com")
spring.mail.port=$(get_env_var APP_MAIL_PORT "587")
spring.mail.username=$(get_env_var APP_MAIL_USERNAME "email@gmail.com")
spring.mail.password=$(get_env_var APP_MAIL_PASSWORD "password")
spring.mail.properties.mail.smtp.auth=$(get_env_var APP_MAIL_SMTP_AUTH "true")
spring.mail.properties.mail.smtp.starttls.enable=$(get_env_var APP_MAIL_SMTP_STARTTLS_ENABLE "true")

#Email subject for the confirmation e-mails
mail.confirm-subject = $(get_env_var APP_MAIL_CONFIRM_SUBJECT "Exporta\u00e7\u00e3o de busca no [AppName]")
#Confirmation message sent when the file was immediately downloaded. Some HTML is fine (mail
#content type is set to 'text/html')
mail.ready-msg = $(get_env_var APP_MAIL_READY_MSG "Prezado usu\u00e1rio,<br><br>Seu arquivo CSV foi criado com \u00eaxito.<br><br>Atenciosamente,<br>Equipe [AppName].")
#Confirmation message sent when the export was requested but the number of records is too large,
#so the download link will be sent later in another email. It is split into two parts so the file
#creation time estimate can be inserted between them in the mail body. Some HTML is fine
mail.wait-msg-top = $(get_env_var APP_MAIL_WAIT_MSG_TOP "Prezado usu\u00e1rio,<br><br>Sua solicita\u00e7\u00e3o de exporta\u00e7\u00e3o foi recebida e o arquivo CSV est\u00e1 sendo criado. Devido \u00e0 quantidade de registros, o arquivo dever\u00e1 levar alguns minutos para ficar pronto. Voc\u00ea receber\u00e1 outro e-mail com o link para download quando estiver conclu\u00eddo.<br><br>Atenciosamente,<br>Equipe [AppName].")
mail.wait-mg-bottom = $(get_env_var APP_MAIL_WAIT_MG_BOTTOM "para ficar pronto. Voc\u00ea receber\u00e1 outro e-mail com o link para download quando estiver conclu\u00eddo.<br><br>Atenciosamente,<br>Equipe [AppName].")
#Email subject for the download link email
mail.link-subject = $(get_env_var APP_MAIL_LINK_SUBJECT "Download do arquivo CSV")
#Download link email message. It is split into two parts so the download link can be inserted
#between them in the mail body. Some HTML is fine
mail.link-msg-top = $(get_env_var APP_MAIL_LINK_MSG_TOP "Prezado usu\u00e1rio,<br><br>Seu arquivo CSV est\u00e1 pronto e pode ser baixado atrav\u00e9s do link")
mail.link-msg-bottom = $(get_env_var APP_MAIL_LINK_MSG_BOTTOM "<br><br>O arquivo ficar\u00e1 dispon\u00edvel para download por 24 horas.<br><br>Atenciosamente,<br>Equipe [AppName].")

##Service delay parameters##

#Creation time per record without abstract, in ms
time.short-record = $(get_env_var APP_TIME_SHORT_RECORD "1.4")
#Creation time per record with abstract, in ms
time.long-record = $(get_env_var APP_TIME_LONG_RECORD "2.8")
#Server delay factor. Total creation time will be = (total records * time per record) * delay factor
time.server-delay = $(get_env_var APP_TIME_SERVER_DELAY "1.5")
#Labels for time units and the conjunction linking them in a formatted time duration
time.units = $(get_env_var APP_TIME_UNITS '{"day":"dia(s)","hour":"hora(s)","minute":"minuto(s)","conjunction":"e"}')

##Service host properties##
server.host = $(get_env_var APP_SERVER_HOST "http://localhost")
server.port = $(get_env_var APP_SERVER_PORT "8081")
# Configuração da exportação RIS
file.ris=$(get_env_var APP_FILE_RIS '{"ty":"format","au":"author_facet","py":"publishDate","ti":"title","la":"dc.language.iso.fl_str_mv","ab":"description","di":"identifier_str_mv","ur":"dc.identifier.uri.fl_str_mv","jo":"reponame_str","issn":"dc.identifier.issn.pt_BR.fl_str_mv","isbn":"dc.identifier.isbn.pt_BR.fl_str_mv","k1":"topic"}')
EOF

  echo "Generated ${APP_PROPS_FILE} from ${ENV_FILE}."
}

export_runtime_env() {
  ensure_env_file

  local project_name
  local app_port

  project_name="${COMPOSE_PROJECT_NAME:-$(get_env_var COMPOSE_PROJECT_NAME vufind-bulk-downloader)}"
  app_port="${BULK_DOWNLOADER_PORT:-$(get_env_var BULK_DOWNLOADER_PORT 8081)}"

  export COMPOSE_PROJECT_NAME="${project_name}"
  export BULK_DOWNLOADER_PORT="${app_port}"
}

dc() {
  export_runtime_env
  ensure_compose_file_exists
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" "$@"
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

start_environment() {
  dc up -d bulk-downloader
  echo "Bulk downloader: http://localhost:${BULK_DOWNLOADER_PORT}"
}

recreate_environment() {
  dc up -d --force-recreate bulk-downloader
  echo "Bulk downloader: http://localhost:${BULK_DOWNLOADER_PORT}"
}

cmd="${1:-help}"
if [ "$#" -gt 0 ]; then
  shift
fi

case "${cmd}" in
  help|-h|--help)
    usage
    ;;

  generate-config)
    generate_application_properties
    ;;

  install)
    ensure_docker_installed
    generate_application_properties
    build_images false
    start_environment
    ;;

  update)
    ensure_docker_installed
    generate_application_properties
    build_images true
    recreate_environment
    ;;

  rebuild)
    ensure_docker_installed
    generate_application_properties
    build_images false
    recreate_environment
    ;;

  restart)
    ensure_docker_installed
    dc restart bulk-downloader
    ;;

  start)
    ensure_docker_installed
    dc start bulk-downloader
    ;;

  stop)
    ensure_docker_installed
    dc stop bulk-downloader
    ;;

  logs)
    ensure_docker_installed
    if [ "$#" -eq 0 ]; then
      dc logs -f bulk-downloader
    else
      dc logs "$@"
    fi
    ;;

  health)
    ensure_docker_installed
    generate_application_properties
    solr_server_url=""
    solr_server_url="$(get_app_property "solr.server.url")"

    dc ps
    echo
    curl -fsS -o /dev/null -w "Bulk:   http://localhost:${BULK_DOWNLOADER_PORT} -> %{http_code}\n" "http://localhost:${BULK_DOWNLOADER_PORT}/" || true
    if [ -n "${solr_server_url}" ]; then
      curl -fsS -o /dev/null -w "Solr:   ${solr_server_url}/select?q=*:*&rows=0 -> %{http_code}\n" "${solr_server_url}/select?q=*:*&rows=0" || true
    else
      echo "Solr:   solr.server.url not found in src/main/resources/application.properties"
    fi
    ;;

  shell)
    ensure_docker_installed
    dc exec bulk-downloader sh
    ;;

  *)
    usage
    exit 1
    ;;
esac
