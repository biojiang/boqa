package sonumina.b4oweb.client.raphael;

public abstract class AnimationCallback {
  public abstract void onComplete();
  static public void fire(AnimationCallback c) {
	c.onComplete();
  }
}

