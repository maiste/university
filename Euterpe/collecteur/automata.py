from enum import Enum
import re

class State(Enum):
    INIT = 1
    FO_BRACK = 2
    SO_BRACK = 3
    HOLE = 4
    FC_BRACK = 5
    SC_BRACK = 6

class Automata:
    def __init__(self, f_token="{", s_token ="{", fc_token="}", sc_token="}"):
        self.state = State.INIT
        self.pre_state = State.INIT
        self.level = 0
        self.in_hole = False
        self.prime_o = f_token
        self.sec_o = s_token
        self.prime_c = fc_token
        self.sec_c = sc_token

    def one_step(self, c):
        self.pre_state = self.state
        if self.state == State.INIT:
            if c == self.prime_o:
                self.state = State.FO_BRACK
                self.in_hole = True
            else:
                self.state = State.INIT
                self.in_hole = False
        elif self.state == State.FO_BRACK:
            if c == self.sec_o:
                self.state = State.SO_BRACK
                self.level = self.level + 1
            elif c == self.prime_c:
                if self.level > 0:
                    self.state = State.FC_BRACK
                else:
                    self.state = State.INIT
                    self.in_hole = False
            else:
                if self.level > 0:
                    self.state = State.HOLE
                else:
                    self.state = State.INIT
                    self.in_hole = False
        elif self.state == State.SO_BRACK:
            if c == self.prime_o:
                self.state = State.FO_BRACK
                self.in_hole = True
            elif c == self.prime_c:
                self.state = State.FC_BRACK
            else:
                self.state = State.HOLE
        elif self.state == State.HOLE:
            if c == self.prime_o:
                self.state = State.FO_BRACK
                self.in_hole = True
            elif c == self.prime_c:
                self.state = State.FC_BRACK
            else:
                self.state = State.HOLE
        elif self.state == State.FC_BRACK:
            if c == self.prime_o:
                self.state = State.FO_BRACK
                self.in_hole = True
            elif c == self.prime_c:
                self.state = State.SC_BRACK
                self.level = self.level - 1
            else:
                self.state = State.HOLE
        elif self.state == State.SC_BRACK:
            if c == self.prime_o:
                self.state = State.FO_BRACK
                self.in_hole = True
            elif c == self.prime_c:
                if self.level == 0:
                    self.state = State.INIT
                    self.in_hole = False
                else:
                    self.state = State.FC_BRACK
            else:
                if self.level ==  0:
                    self.state = State.INIT
                    self.in_hole = False
                else:
                    self.state = State.HOLE
        return self.state

    def ignore(self):
        return self.in_hole

    def must_add_bracket(self):
        return (self.pre_state == State.FO_BRACK and self.state == State.INIT)


class Remover:
    def __init__(self, title):
        regex = r"={2,3}[ ]?" + title +"[ ]?={2,3}"
        self.re = re.compile(regex)
        self.ignore = False

    def one_step(self, line):
        if self.re.match(line) is not None:
            self.ignore = True
        elif self.ignore == True and line.find("==", 0, 4) != -1:
                self.ignore = False
        return self.ignore
