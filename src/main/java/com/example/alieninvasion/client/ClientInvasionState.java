package com.example.alieninvasion.client;

/** Клиентское состояние вторжения, синхронизируемое с сервера пакетами. */
public final class ClientInvasionState {
    /** true после победы (Мать Роя пала) — HUD вторжения полностью скрывается. */
    public static volatile boolean victoryShown = false;

    private ClientInvasionState() {}
}
