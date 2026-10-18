package ru.shitneba;

public interface GameUI {
   void toast(String var1);

   void banner(String var1, boolean var2, String var3);

   void hint(String var1);

   void comm(String var1, String var2);

   void startHud();

   void over(boolean var1);

   void result(boolean var1);

   void strikeOver();

   void duelResult(boolean var1, boolean var2, String var3);
}
