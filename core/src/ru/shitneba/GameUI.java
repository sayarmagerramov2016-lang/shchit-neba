package ru.shitneba;

/** Методы интерфейса, которые вызывает логика боя. */
public interface GameUI {
    void toast(String m);
    void banner(String t, boolean boss, String sub);
    void hint(String t);
    void comm(String who, String text);
    void startHud();
    void over(boolean rec);
    void result(boolean win);
    void strikeOver();
    void duelResult(boolean lostLink, boolean win, String reason);
    /** Событие для девчонки-оператора: hit, kill, boss, wave, low, lose, abil, save. */
    void opEvent(String ev);
}
