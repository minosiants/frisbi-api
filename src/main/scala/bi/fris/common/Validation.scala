package bi.fris.common

trait Validation {
def validText(txt:String, length:Int=200) = !txt.isEmpty && txt.length() <= length
    def validEmail(email:String):Boolean = emailRegex.findFirstMatchIn(email).map(e => true).getOrElse(false) 
    private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
}