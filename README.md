#![ParrotSMTP Logo](https://github.com/vgorin/parrot-smtp/raw/master/src/main/webapp/parrot-smtp_logo1.gif)

**ParrotSMTP** is a tiny SMTP service which accepts and stores any email sent to existing/non-existing recipient within [parrotsmtp.com](http://parrotsmtp.com) domain. Use [Burn After Reading](http://parrotsmtp.com/bar_client.shtml) client to read it.

The project demonstrates the use of few simple but functional technologies:

* A very basic [SMTP implementation](https://github.com/vgorin/parrot-smtp/blob/master/src/main/java/com/parrotsmtp/service/SMTPService.java) which responds **250 OK** on almost everything you say it, just like a *parrot*. 
* Old, good, but nowadays forgotten [Server Side Includes (SSI)](https://en.wikipedia.org/wiki/Server_Side_Includes). Take a look at *.shtml and *.txt files [here](https://github.com/vgorin/parrot-smtp/tree/master/src/main/webapp).
* The simplest [LRU](https://en.wikipedia.org/wiki/Cache_replacement_policies#LRU) map [implementation](https://github.com/vgorin/parrot-smtp/blob/master/src/main/java/com/parrotsmtp/util/LRUMap.java).

ParrotSTMP logo design by [Tatyana Boyko](https://www.linkedin.com/in/tboyko).
