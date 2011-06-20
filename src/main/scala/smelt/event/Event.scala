package smelt.event

case class Header(name:String, value:String)

object Headers {
  def routing_key = "routing_key"
  def content_type = "content_type"
}

case class Event(payload:Array[Byte], headers:Header*) {
}