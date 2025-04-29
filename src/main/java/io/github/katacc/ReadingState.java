package io.github.katacc;

public enum ReadingState {
    FADER {
        public ReadingState nextState() {
            return APPLICATION;
        }
    },

    APPLICATION {
        public ReadingState nextState() {
            return BLANK;
        }
    },

    BLANK {
        public ReadingState nextState() {
            return FADER;
        }
    },

    DONE {
        public ReadingState nextState() {
            return FADER;
        }
    };

    public abstract ReadingState nextState();
}
