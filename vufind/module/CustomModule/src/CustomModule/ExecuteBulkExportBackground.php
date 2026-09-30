<?php

class ExecuteBulkExportBackground
{
    protected $email;
    protected $serviceUrl;
    protected $paramString;
    protected $totalRecords;
    protected $hasAbstract;
    protected $encoding;
    protected $type;

    public function __construct($paramsFile)
    {
        // Params are passed through a temporary JSON file written by
        // BulkExportController, which is removed right after reading
        $paramsArray = json_decode(file_get_contents($paramsFile), true);
        unlink($paramsFile);

        if (!is_array($paramsArray)) {
            throw new Exception(sprintf('Invalid export params.'));
        }

        $this->email = $paramsArray['email'];
        $this->serviceUrl = $paramsArray['serviceUrl'];
        $this->paramString = $paramsArray['paramString'];
        $this->totalRecords = intval($paramsArray['totalRecords']);
        $this->hasAbstract = filter_var(
            $paramsArray['hasAbstract'],
            FILTER_VALIDATE_BOOLEAN,
        );
        $this->encoding = $paramsArray['encoding'];
        $this->type = $paramsArray['type'];
    }

    public function execute()
    {
        // Call the bulk downloader service to create the CSV file
        $params = [
            'queryString' => $this->paramString,
            'download' => 'false',
            'totalRecords' => $this->totalRecords,
            'hasAbstract' => $this->hasAbstract,
            'encoding' => $this->encoding,
            'userEmail' => $this->email,
            'type' => $this->type,
        ];
        $options = [
            'http' => [
                'header' =>
                    "Content-type: application/x-www-form-urlencoded\r\n",
                'method' => 'POST',
                'content' => http_build_query($params),
            ],
        ];

        $context = stream_context_create($options);
        $response = file_get_contents($this->serviceUrl, false, $context);

        if ($response === false) {
            throw new Exception(sprintf('Unexpected exception.'));
        }
    }
}

$obj = new ExecuteBulkExportBackground($argv[1]);
$obj->execute();

?>
