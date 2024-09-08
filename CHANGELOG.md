
# Change Log

## 2024-09-08 version 0.8.1

- Added fix for type Char

## 2024-06-23 version 0.8.0

- Fix on records with several boolean fields
- Added support for Enums, thanks to [thewildllama]

## 2023-04-17 version 0.7.0

- Small fix when using ids with integer

## 2023-04-14 version 0.6.0

- Added support for records with several constructors

## 2023-01-11 version 0.5.0

- Added support for TEXT types
- Fixed bug when saving a record without id

## 2022-05-31 version 0.4.0

- Added support for views
- Allow different order in object fields and table fields

## 2022-05-22 version 0.3.0

- Checked support for Snowflake, it works as long as the table has no primary keys
- Fixed bug on Time type
- Fixed bug on tables with slightly different names than classes

## 2022-04-10 version 0.2.0

- Added initial support for Postgresql, thanks to [PabloGrisafi]
- Deletion now returns true or false
- Added fluent API, based on [Benjiql] idea

## 2022-03-19 version 0.1.0

- Yorm is born, just working with Mysql


[PabloGrisafi]: <https://github.com/pablogrisafi1975>
[Benjiql]: <https://github.com/benjiman/benjiql>
[thewildllama]: <https://github.com/thewildllama>
