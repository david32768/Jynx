package jynx2asm;

import java.util.Deque;
import java.util.EnumSet;
import java.util.Optional;

import static jynx.Global.LOG;
import static jynx.Message.M140;
import static jynx.Message.M402;
import static jynx.Message.M90;

import jvm.AccessFlag;
import jynx.LogIllegalStateException;
import jynx.ReservedWord;

public interface TokenDeque {

    public Token firstToken();

    public Deque<Token> getDeque();
    
    public default Token peekToken() {
        Token token = getDeque().peekFirst();
        if (token == null) {
            throw new LogIllegalStateException(M140);  // "reading next token after reaching last"
        } else {
            return token;
        }
    }

    public default Token nextToken() {
        Deque<Token> tokens = getDeque();
        if (tokens.isEmpty()) {
            throw new LogIllegalStateException(M140);  // "reading next token after reaching last"
        }
        return tokens.removeFirst();
    }

    public default void insert(Token insert) {
        if (insert == Token.END_TOKEN) {
            throw new LogIllegalStateException(M402);  // "cannot insert end_token"
        }
        getDeque().addFirst(insert);
    }

    public default void noMoreTokens() {
        Token token = getDeque().peekFirst();
        if (token == null || token == Token.END_TOKEN) {
        } else {
            LOG(M90,token);    // "unused tokens - starting at %s"
            skipTokens();
        }
    }

    public default void skipTokens() {
        getDeque().clear();
    }
    
    public default Token lastToken()  {
        Token token = nextToken();
        noMoreTokens();
        return token;
    }

    public default String after(ReservedWord rw)   {
        Token token = nextToken();
        token.mustBe(rw);
        token = nextToken();
        return rw.token2string(token);
    }

    public default String optAfter(ReservedWord rw) {
        assert rw.isOptional();
        Token token = peekToken();
        if (token.is(rw)) {
            nextToken();
            token = nextToken();
            return rw.token2string(token);
        }
        return null;
    }

    public default EnumSet<AccessFlag> getAccFlags()  {
          EnumSet<AccessFlag> accflags = EnumSet.noneOf(AccessFlag.class);
          while (true) {
              Token token = peekToken();
              Optional<AccessFlag> afopt = AccessFlag.fromString(token.asString());
              if (afopt.isPresent()) {
                  nextToken();
                  accflags.add(afopt.get());
              } else {
                  break;
              }
          }
          return accflags;
    }

}
