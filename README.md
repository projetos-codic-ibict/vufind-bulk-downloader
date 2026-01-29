# VuFind Search Results Bulk Downloader

A comprehensive solution for exporting VuFind search results into CSV or RIS format. This component includes a Java-based backend service for processing large exports and a VuFind module/theme customization for the frontend integration.


**Idiomas / Languages:**
- [English (current)](#vufind-search-results-bulk-downloader)
- [Español](#vufind-search-results-bulk-downloader-descargador-masivo-de-resultados)
- [Português](#vufind-search-results-bulk-downloader-download-em-massa-de-resultados)


## Prerequisites
- **Java**: JRE/JDK 17 or higher.
- **Maven**: 3.8 or higher. For building the backend service.
- **PHP**: 8.2 or higher (standard with VuFind 7+).
- **VuFind**: Version 7.x or higher.


## 1. Bulk Downloader Setup (Java/Spring Boot)

The backend service is a Java application that performs Solr queries and generates CSV files.

### Installation
1. **Clone this repository.**
2. **Prepare the configuration files**:
   - Copy `src/main/resources/application.properties.model` to `application.properties` and edit the values.
   - Edit `bulk-downloader.conf` as needed (e.g., memory settings).
3. **Build the project using Maven**:
   ```bash
   ./build.sh
   ```
   This generates `bulk-downloader.jar` in the root directory.

### Running the Service
**Manual execution**:
```bash
java -jar bulk-downloader.jar
```

**System service (Linux)**:
```bash
sudo ln -s $(pwd)/bulk-downloader.jar /etc/init.d/bulk-downloader
sudo /etc/init.d/bulk-downloader start
```
The service will be available at `http://host:port/`.


## 2. VuFind Integration (PHP)

Follow these steps within your `<VUFIND_HOME>` directory to integrate the export functionality.

### Step 1: Configuration
1. **Copy the configuration file**: Copy `vufind/bulkexport.ini` to `<VUFIND_HOME>/local/config/vufind/`.
2. **Adjust settings**: Edit `bulkexport.ini` to update the `serviceUrl`, `auxServUrl`, and your Google ReCaptcha keys in the `[Captcha]` section.

### Step 2: Theme Customization
1. **Copy templates**: Copy the `vufind/themes/custom_theme/templates/bulkexport` folder to your custom theme's `templates/` directory.
2. **Update search results**:
   - If `search/results.phtml` doesn't exist in your theme, copy it from the base theme.
   - Insert the "Export CSV" link code snippet (found in this repository's `vufind/themes/custom_theme/templates/search/results.phtml`) into your template.

### Step 3: Custom Module Logic
1. **Controller**: Copy `vufind/module/CustomModule/src/CustomModule/Controller/BulkExportController.php` to `module/<YOUR_MODULE>/src/<YOUR_MODULE>/Controller/`.
2. **Support Classes**: Copy `vufind/module/CustomModule/src/CustomModule/BulkExportConfirm.php` and `vufind/module/CustomModule/src/CustomModule/ExecuteBulkExportBackground.php` to `module/<YOUR_MODULE>/src/<YOUR_MODULE>/`.
3. **Refactoring**: 
   > [!IMPORTANT]
   > In all copied `.php` files, update the `namespace` and class paths to match your custom module name.
4. **Routes**: Merge the configurations from `vufind/module/CustomModule/config/module.config.php` into your custom module's `module.config.php`.

### Step 4: Language Files
Copy the translation strings from this repository's `vufind/languages/` folder (e.g., `en.ini`, `es.ini`, `pt-br.ini`) into the corresponding files in `<VUFIND_HOME>/languages/`.


## 3. Troubleshooting
If the new routes or translations are not recognized, clear the VuFind cache:
```bash
rm -rf <VUFIND_HOME>/local/cache/configs/*
rm -rf <VUFIND_HOME>/local/cache/languages/*
```

> [!TIP]
> If downloads fail for very large result sets, increase the `timeout` setting in `bulkexport.ini`.

*Developed by LA Referencia / IBICT*

---
[ES]
# VuFind Search Results Bulk Downloader (Descargador Masivo de Resultados)

Una solución integral para exportar resultados de búsqueda de VuFind a formato CSV o RIS. Este componente incluye un servicio backend basado en Java para procesar exportaciones grandes y una personalización del módulo/tema de VuFind para la integración frontend.



## Requisitos Previos
- **Java**: JRE/JDK 17 o superior.
- **Maven**: 3.8 o superior. Para construir el servicio backend.
- **PHP**: 8.2 o superior (estándar con VuFind 7+).
- **VuFind**: Versión 7.x o superior.


## 1. Configuración del Bulk Downloader (Java/Spring Boot)

El servicio backend es una aplicación Java que realiza consultas a Solr y genera archivos CSV.

### Instalación
1. **Clonar este repositorio.**
2. **Preparar los archivos de configuración**:
   - Copiar `src/main/resources/application.properties.model` a `application.properties` y editar los valores.
   - Editar `bulk-downloader.conf` según sea necesario (p. ej., ajustes de memoria).
3. **Construir el proyecto usando Maven**:
   ```bash
   ./build.sh
   ```
   Esto genera `bulk-downloader.jar` en el directorio raíz.

### Ejecución del Servicio
**Ejecución manual**:
```bash
java -jar bulk-downloader.jar
```

**Servicio del sistema (Linux)**:
```bash
sudo ln -s $(pwd)/bulk-downloader.jar /etc/init.d/bulk-downloader
sudo /etc/init.d/bulk-downloader start
```
El servicio estará disponible en `http://host:port/`.


## 2. Integración con VuFind (PHP)

Siga estos pasos dentro de su directorio `<VUFIND_HOME>` para integrar la funcionalidad de exportación.

### Paso 1: Configuración
1. **Copiar el archivo de configuración**: Copie `vufind/bulkexport.ini` a `<VUFIND_HOME>/local/config/vufind/`.
2. **Ajustar la configuración**: Edite `bulkexport.ini` para actualizar el `serviceUrl`, `auxServUrl`, y sus claves de Google ReCaptcha en la sección `[Captcha]`.

### Paso 2: Personalización del Tema
1. **Copiar plantillas**: Copie la carpeta `vufind/themes/custom_theme/templates/bulkexport` al directorio `templates/` de su tema personalizado.
2. **Actualizar los resultados de búsqueda**:
   - Si `search/results.phtml` no existe en su tema, cópielo del tema base.
   - Inserte el fragmento de código del enlace "Exportar CSV" (que se encuentra en `vufind/themes/custom_theme/templates/search/results.phtml` de este repositorio) en su plantilla.

### Paso 3: Lógica del Módulo Personalizado
1. **Controlador**: Copie `vufind/module/CustomModule/src/CustomModule/Controller/BulkExportController.php` a `module/<SU_MODULO>/src/<SU_MODULO>/Controller/`.
2. **Clases de Soporte**: Copie `vufind/module/CustomModule/src/CustomModule/BulkExportConfirm.php` y `vufind/module/CustomModule/src/CustomModule/ExecuteBulkExportBackground.php` a `module/<SU_MODULO>/src/<SU_MODULO>/`.
3. **Refactorización**: 
   > [!IMPORTANT]
   > En todos los archivos `.php` copiados, actualice el `namespace` y las rutas de las clases para que coincidan con el nombre de su módulo personalizado.
4. **Rutas**: Combine las configuraciones de `vufind/module/CustomModule/config/module.config.php` en el archivo `module.config.php` de su módulo personalizado.

### Paso 4: Archivos de Idioma
Copie las cadenas de traducción de la carpeta `vufind/languages/` de este repositorio (p. ej., `en.ini`, `es.ini`, `pt-br.ini`) en los archivos correspondientes en `<VUFIND_HOME>/languages/`.


## 3. Solución de Problemas
Si las nuevas rutas o traducciones no son reconocidas, limpie la caché de VuFind:
```bash
rm -rf <VUFIND_HOME>/local/cache/configs/*
rm -rf <VUFIND_HOME>/local/cache/languages/*
```

> [!TIP]
> Si las descargas fallan para conjuntos de resultados muy grandes, aumente el ajuste `timeout` en `bulkexport.ini`.

*Desarrollado por LA Referencia / IBICT*

---
[PT-BR]
# VuFind Search Results Bulk Downloader (Download em Massa de Resultados)

Uma solução abrangente para exportar resultados de pesquisa do VuFind para os formatos CSV ou RIS. Este componente inclui um serviço de backend baseado em Java para processar grandes exportações e uma personalização de módulo/tema do VuFind para a integração com o frontend.


## Pré-requisitos
- **Java**: JRE/JDK 17 ou superior.
- **Maven**: 3.8 ou superior. Para compilar o serviço de backend.
- **PHP**: 8.2 ou superior (padrão no VuFind 7+).
- **VuFind**: Versão 7.x ou superior.


## 1. Configuração do Bulk Downloader (Java/Spring Boot)

O serviço de backend é uma aplicação Java que realiza consultas no Solr e gera arquivos CSV.

### Instalação
1. **Clone este repositório.**
2. **Prepare os arquivos de configuração**:
   - Copie `src/main/resources/application.properties.model` para `application.properties` e edite os valores.
   - Edite `bulk-downloader.conf` conforme necessário (ex: configurações de memória).
3. **Compile o projeto usando Maven**:
   ```bash
   ./build.sh
   ```
   Isso gera o arquivo `bulk-downloader.jar` no diretório raiz.

### Executando o Serviço
**Execução manual**:
```bash
java -jar bulk-downloader.jar
```

**Serviço do sistema (Linux)**:
```bash
sudo ln -s $(pwd)/bulk-downloader.jar /etc/init.d/bulk-downloader
sudo /etc/init.d/bulk-downloader start
```
O serviço estará disponível em `http://host:port/`.


## 2. Integração com o VuFind (PHP)

Siga estes passos dentro do seu diretório `<VUFIND_HOME>` para integrar a funcionalidade de exportação.

### Passo 1: Configuração
1. **Copie o arquivo de configuração**: Copie `vufind/bulkexport.ini` para `<VUFIND_HOME>/local/config/vufind/`.
2. **Ajuste as configurações**: Edite o arquivo `bulkexport.ini` para atualizar o `serviceUrl`, `auxServUrl` e suas chaves do Google ReCaptcha na seção `[Captcha]`.

### Passo 2: Customização do Tema
1. **Copie os templates**: Copie a pasta `vufind/themes/custom_theme/templates/bulkexport` para o diretório `templates/` do seu tema customizado.
2. **Atualize os resultados de busca**:
   - Se o arquivo `search/results.phtml` não existir no seu tema, copie-o do tema base.
   - Insira o trecho de código do link "Exportar CSV" (encontrado em `vufind/themes/custom_theme/templates/search/results.phtml` neste repositório) no seu template.

### Passo 3: Lógica do Módulo Customizado
1. **Controller**: Copie `vufind/module/CustomModule/src/CustomModule/Controller/BulkExportController.php` para `module/<SEU_MODULO>/src/<SEU_MODULO>/Controller/`.
2. **Classes de Suporte**: Copie `vufind/module/CustomModule/src/CustomModule/BulkExportConfirm.php` e `vufind/module/CustomModule/src/CustomModule/ExecuteBulkExportBackground.php` para `module/<SEU_MODULO>/src/<SEU_MODULO>/`.
3. **Refatoração**: 
   > [!IMPORTANT]
   > Em todos os arquivos `.php` copiados, atualize o `namespace` e os caminhos das classes para corresponderem ao nome do seu módulo customizado.
4. **Rotas**: Mescle as configurações de `vufind/module/CustomModule/config/module.config.php` no arquivo `module.config.php` do seu módulo customizado.

### Passo 4: Arquivos de Idioma
Copie as strings de tradução da pasta `vufind/languages/` deste repositório (ex: `en.ini`, `es.ini`, `pt-br.ini`) para os arquivos correspondentes em `<VUFIND_HOME>/languages/`.


## 3. Resolução de Problemas
Se as novas rotas ou traduções não forem reconhecidas, limpe o cache do VuFind:
```bash
rm -rf <VUFIND_HOME>/local/cache/configs/*
rm -rf <VUFIND_HOME>/local/cache/languages/*
```

> [!TIP]
> Se os downloads falharem para conjuntos de resultados muito grandes, aumente a configuração de `timeout` no arquivo `bulkexport.ini`.


*Desenvolvido por LA Referencia / IBICT*
