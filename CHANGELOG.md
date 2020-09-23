# Changelog

## 1.1.2

* Bugfix: Fehler "stream is closed" bei SVG-Druck: URL nur einmalig transcodieren, da "paint" öfter gecalled werden kann (z.B. overlay) 

## 1.1.1

* Bugfix: Durch fehlende Dependency konnten inline base64 images nicht erzeugt werden.

## 1.1.0

* Neu: Umstellung auf maven-Projektstruktur / maven-build
* Neu: Es werden nun SVG-Grafiken unterstützt, wenn diese per `<object>`-Tag im HTML eingebunden werden:

`<object type="image/svg+xml" data="URL"></object>`

* Bugfix: Im Haupt-Template der IndiPDF-Integration fehlte das schließende `</style>`-Tag. 
Wurde von JTidy bisher automatisch korrigiert.

## 1.0.0

* Legacy-Stand vor Übernahme nach maven