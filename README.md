# Bruno's

Bruno fa ottimi pranzi di lavoro, ma vuole una prenotazione precisa.

### Prerequisites

* Java 21
* Maven 3.8+

### Frontend

* checkout frontend repo
* build with
```
npm install
npm run build
```
* copy `<frontend>\dist\brunos-fe\browser\` to `<backend>\src\main\resources\static`

### Build

`mvn package`

### Running

`java --enable-preview -Dspring.profiles.active=prod -jar brunos-1.0.0.jar`

