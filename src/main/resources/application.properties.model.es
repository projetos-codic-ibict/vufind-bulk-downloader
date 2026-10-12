##Solr query properties##

#Server address
solr.server.url = http://localhost:8983/solr/biblio

##CSV file properties##

#Directory where files will be saved to
file.path=/app/data/
#Separator character
file.sep-char = ,
#Symbol used to separate values in multiple-value fields
file.list-sep = ||
#CSV column headers. This list of fields must be synchronized with the list of
#fields available for export defined in VuFind. If fields are to be aggregated
#in a single column, the header must be set to "null", and the new column header
#defined in the 'file.agg-fields' property below
file.header = {"title":"T\u00edtulo del documento", \
				"publishDate":"A\u00f1o de publicaci\u00f3n", \
				"author":"Autores", \
				"instname_str":"T\u00edtulo de la instituci\u00f3n de origen", \
				"institution":"Sigla de la instituci\u00f3n de origen", \
				"format": "Tipo de documento", \
				"dc.identifier.citation.fl_str_mv":"Referencia"}

#Aggregated fields, which gather multiple fields in a single column as a list
file.agg-fields = {"Directores":"dc.contributor.advisor1.fl_str_mv,dc.contributor.advisor2.fl_str_mv", \
                   "ID Lattes de los directores":"dc.contributor.advisor1Lattes.fl_str_mv,dc.contributor.advisor2Lattes.fl_str_mv", \
                   "Miembros del jurado":"dc.contributor.referee1.fl_str_mv, dc.contributor.referee2.fl_str_mv, dc.contributor.referee3.fl_str_mv, dc.contributor.referee4.fl_str_mv, dc.contributor.referee5.fl_str_mv", \
                   "ID Lattes de los miembros del jurado":"dc.contributor.referee1Lattes.fl_str_mv,dc.contributor.referee2Lattes.fl_str_mv,dc.contributor.referee3Lattes.fl_str_mv,dc.contributor.referee4Lattes.fl_str_mv,dc.contributor.referee5Lattes.fl_str_mv"}

#Message to be show for empty fields
file.null-msg = No informado por la instituci\u00f3n
#Fields for which the message should not be shown if empty
file.no-msg-fields = dc.contributor.co.fl_str_mv

##Email properties##
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=email@gmail.com
spring.mail.password=password
# Address displayed in the From header; it may differ from the SMTP username.
spring.mail.sender=sender@example.com
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

#Email subject for the confirmation e-mails
mail.confirm-subject = Exportaci\u00f3n de b\u00fasqueda en LA Referencia
#Confirmation message sent when the file was immediately downloaded. Some HTML is fine (mail
#content type is set to 'text/html')
mail.ready-msg = Estimado usuario:<br><br>Su archivo CSV se ha creado correctamente.<br><br>Atentamente,<br>Equipo de LA Referencia.
#Confirmation message sent when the export was requested but the number of records is too large,
#so the download link will be sent later in another email. It is split into two parts so the file
#creation time estimate can be inserted between them in the mail body. Some HTML is fine
mail.wait-msg-top = Estimado usuario:<br><br>Hemos recibido su solicitud de exportaci\u00f3n y el archivo CSV se est\u00e1 creando. Debido a la cantidad de registros, el archivo tardar\u00e1 unos minutos en estar listo. Recibir\u00e1 otro correo electr\u00f3nico con el enlace de descarga cuando se complete.<br><br>Atentamente,<br>Equipo de LA Referencia.
mail.wait-mg-bottom = para estar listo. Recibir\u00e1 otro correo electr\u00f3nico con el enlace de descarga cuando se complete.<br><br>Atentamente,<br>Equipo de LA Referencia.
#Email subject for the download link email
mail.link-subject = Descarga del archivo CSV
#Download link email message. It is split into two parts so the download link can be inserted
#between them in the mail body. Some HTML is fine
mail.link-msg-top = Estimado usuario:<br><br>Su archivo CSV est\u00e1 listo y puede descargarlo a trav\u00e9s del enlace
mail.link-msg-bottom = <br><br>El archivo estar\u00e1 disponible para su descarga durante 24 horas.<br><br>Atentamente,<br>Equipo de LA Referencia.

##Service delay parameters##

#Creation time per record without abstract, in ms
time.short-record = 1.4
#Creation time per record with abstract, in ms
time.long-record = 2.8
#Server delay factor. Total creation time will be = (total records * time per record) * delay factor
time.server-delay = 1.5
#Labels for time units and the conjunction linking them in a formatted time duration
time.units = {"day":"d\u00eda(s)", \
			  "hour":"hora(s)", \
			  "minute":"minuto(s)", \
			  "conjunction":"y"}

##Service host properties##
server.host = http://localhost:8081
server.port = 8081
# Configuraci\u00f3n de la exportaci\u00f3n RIS
file.ris={"ty":"format", "au":"author_facet","py":"publishDate","ti":"title","la":"dc.language.iso.fl_str_mv", "ab":"description","di":"identifier_str_mv","ur":"dc.identifier.uri.fl_str_mv", "jo":"reponame_str", "issn":"dc.identifier.issn.pt_BR.fl_str_mv", "isbn":"dc.identifier.isbn.pt_BR.fl_str_mv", "k1":"topic"}
