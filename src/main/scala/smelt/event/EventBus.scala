package smelt.event

trait EventBus {
  def publish(event:Event)
}