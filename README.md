Блоговый движок
=============================

Блоговый движок на Java - Spring-Framework

[![Build Status](https://github.com/yiisoft/yii/workflows/build/badge.svg)](https://github.com/akrafit/thesis)

Проект написан по принципу трехслойного приложения:
* Клиентский слой - на HTML/CSS + JavaScript; 
* Сервисный слой - на Java - Spring-Framework;
* Слой данных - на MySQL 8.0


Основной стек технологий
------------

* JDK 11.0.13
* Spring Framework 2.1.14
* MySQl 8.0.28
* Lombok 1.18.22
* CAptcha GEnerator 1.0
* Apache Commons Lang  3.12.0
* Fastjson1 Compatible  1.2.80
* Apache Commons FileUpload  1.4
* Imgscalr A Java Image Scaling Library 4.2      

Структура проекта 
-----------
* Main.java - точка входа в приложение со статическими интерфейсами для реализации авторизации и хранения глобальных настроек
* config - содержит класс для статических данных сайта:</br>
  настройки для отправки email</br>
  конфигурация доступа к изображениям в файловой системе
* controller - содержит основные REST контроллеры: </br>
  ApiAuthController - работает с запросами по пользователю </br>
  ApiPostController - работает с запросами по записями </br>
  ApiGeneralController - работает с общими запросами от фронта</br>
  DefaultController - открывает основной index.html
* model - содержит описание всех классов и Enum-ов проекта
* repo - содержит интерфейсы для работы с базой данных
* service - содержит сервисы с реализацией бизнес логики по: пользователям, записям, основным настройкам, отправки почты
* specification - содержит единственный класс PostSpecification реализующий поиск по всем записям
* resources - содержит файл конфигурации application.yml, а так же фронт приложения.


Инструкция для запуска
-----------

Для запуска приложения необходимо в файле application.yml изменить все данные: 
* url адрес вашего хоста
* datasource.url адрес вашей базы данных
* имя и пароль от базы данных
* mail изменить параметры вашего почтового сервера

Пример сайта
-----------
http://185.178.46.24
