package bi.fris
package common

import akka.http.scaladsl.model.{MediaTypes, ContentType, HttpEntity, HttpResponse}


object Html extends Html

trait Html {

  val windowClose = new HttpResponse(entity = HttpEntity(string =
    """
      |<html>
      |<head>
      |<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
      |<title>Insert title here</title>
      |</head>
      |<body>
      |  <script  type="text/javascript">
      |      window.close();
      |  </script>
      |</body>
      |</html>
    """.stripMargin, contentType = ContentType(MediaTypes.`text/html`)))

}
