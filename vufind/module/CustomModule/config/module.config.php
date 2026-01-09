<?php

return [
    'controllers' => [
        'factories' => [
            'CustomModule\\Controller\\BulkExportController' =>
                'VuFind\\Controller\\AbstractBaseFactory',
        ],
        'aliases' => [
            'BulkExport' => 'CustomModule\\Controller\\BulkExportController',
            'bulkexport' => 'CustomModule\\Controller\\BulkExportController',
        ],
    ],
    'router' => [
        'routes' => [
            'bulkexport-home' => [
                'type' => 'Laminas\\Router\\Http\\Literal',
                'options' => [
                    'route' => '/bulkexport/home',
                    'defaults' => [
                        'controller' => 'BulkExport',
                        'action' => 'Home',
                    ],
                ],
            ],
            'bulkexport-csv' => [
                'type' => 'Laminas\\Router\\Http\\Literal',
                'options' => [
                    'route' => '/bulkexport/csv',
                    'defaults' => [
                        'controller' => 'BulkExport',
                        'action' => 'CSV',
                    ],
                ],
            ],
            'bulkexport-download' => [
                'type' => 'Laminas\\Router\\Http\\Literal',
                'options' => [
                    'route' => '/bulkexport/download',
                    'defaults' => [
                        'controller' => 'BulkExport',
                        'action' => 'Download',
                    ],
                ],
            ],
        ],
    ],
];
