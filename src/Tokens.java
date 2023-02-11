public class Tokens {
    static final int errorToken = 0;

    static final int timesToken = 1;
    static final int divToken = 2;

    static final int plusToken = 11;
    static final int minusToken = 12;

    static final int eqlToken = 20;
    static final int neqToken = 21;
    static final int lssToken = 22;
    static final int geqToken = 23;
    static final int leqToken = 24;
    static final int gtrToken = 25;

    static final int periodToken = 30;
    static final int commaToken = 31;
    static final int openbracketToken = 32;
    static final int closebracketToken = 34;
    static final int closeParenToken = 35;

    static final int becomesToken = 40;
    static final int thenToken = 41;
    static final int doToken = 42;

    static final int openParenToken = 50;

    static final int number = 60;
    static final int ident = 61;

    static final int semiToken = 70;

    static final int endToken = 80;
    static final int odToken = 81;
    static final int fiToken = 82;

    static final int elseToken = 90;

    static final int letToken = 100;
    static final int callToken = 101;
    static final int ifToken = 102;
    static final int whileToken = 103;
    static final int returnToken = 104;

    static final int varToken = 110;
    static final int arrToken = 111;
    static final int voidToken = 112;
    static final int funcToken = 113;
    static final int procToken = 114;

    static final int beginToken = 150;
    static final int mainToken = 200;
    static final int eofToken = 255;
}