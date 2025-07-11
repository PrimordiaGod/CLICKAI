package com.mycompany.tap;

interface ITapService {
    void tap(int x, int y);
    void swipe(int x1, int y1, int x2, int y2, int durationMs);
    void inputText(String text);
}