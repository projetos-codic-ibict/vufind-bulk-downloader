# VuFind Search Results Bulk Downloader

A comprehensive solution for exporting VuFind search results into CSV or RIS format. This component includes a Java-based backend service for processing large exports and a VuFind module/theme customization for the frontend integration.

---

## Prerequisites
- **Java**: JRE/JDK 17 or higher.
- **Maven**: 3.8 or higher. For building the backend service.
- **PHP**: 8.2 or higher (standard with VuFind 7+).
- **VuFind**: Version 7.x or higher.

---

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

---

## 2. VuFind Integration (PHP)

Follow these steps within your `<VUFIND_HOME>` directory to integrate the export functionality.

### Step 1: Configuration
1. **Copy the configuration file**: Copy `vufind/bulkexport.ini` to `<VUFIND_HOME>/local/config/vufind/`.
2. **Adjust settings**: Edit `bulkexport.ini` to update the `serviceUrl`, `auxServUrl`, and your **Google ReCaptcha** keys in the `[Captcha]` section.

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

---

## 3. Troubleshooting
If the new routes or translations are not recognized, clear the VuFind cache:
```bash
rm -rf <VUFIND_HOME>/local/cache/configs/*
rm -rf <VUFIND_HOME>/local/cache/languages/*
```

> [!TIP]
> If downloads fail for very large result sets, increase the `timeout` setting in `bulkexport.ini`.

---

*Developed by LA Referencia / IBICT*
