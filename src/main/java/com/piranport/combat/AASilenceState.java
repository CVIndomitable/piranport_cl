package com.piranport.combat;

/** 每个玩家实例独立的临时静默状态，不跨重生或服务器会话残留。 */
public final class AASilenceState {
    public static final int DURATION_TICKS = 100;
    private long endsAt;

    public void start(long gameTime) { endsAt = gameTime + DURATION_TICKS; }
    public boolean isActive(long gameTime) { return endsAt > gameTime; }
    public long endsAt() { return endsAt; }
    public void setEndsAt(long endsAt) { this.endsAt = Math.max(0L, endsAt); }
    public void clear() { endsAt = 0L; }
}
