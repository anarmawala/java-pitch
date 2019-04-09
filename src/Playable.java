/**
 * Defines the different items that can be played in the game <i>Rock, Paper, Scissors, Spock and Lizard</i>
 */
public enum Playable {
    /**
     * <pre>
     * <b>Beats</b>     : SPOCK and PAPER
     * <b>Beaten by</b> : ROCK and SCISSORS</pre>
     */
    LIZARD,
    /**
     * <pre>
     * <b>Beats</b>     : SPOCK and ROCK
     * <b>Beaten by</b> : LIZARD and SCISSORS</pre>
     */
    PAPER,
    /**
     * <pre>
     * <b>Beats</b>     : NONE
     * <b>Beaten by</b> : NONE</pre>
     */
    QUESTION,
    /**
     * <pre>
     * <b>Beats</b>     : SCISSORS and LIZARD
     * <b>Beaten by</b> : SPOCK and PAPER</pre>
     */
    ROCK,
    /**
     * <pre>
     * <b>Beats</b>     : PAPER and LIZARD
     * <b>Beaten by</b> : SPOCK and ROCK</pre>
     */
    SCISSORS,
    /**
     * <pre>
     * <b>Beats</b>     : ROCK and SCISSORS
     * <b>Beaten by</b> : LIZARD and PAPER</pre>
     */
    SPOCK;
    
    /**
     * Compare two Client.Playable to figure if one in dominant or neutral
     *
     * <pre>{@code int i = Client.Playable.ROCK.beats(Client.Playable.SCISSORS);}</pre>
     *
     * @param other the Client.Playable to compare against
     * @return the int corresponding to whether the playable is beaten, tied or beat
     */
    public int beats(Playable other) {
        // 0 is neutral, 1 is beat, -1 is lose
        if (other == this && this != QUESTION)
            return 0;
        
        switch (this) {
            case ROCK:
                if (other == SCISSORS || other == LIZARD)
                    return 1;
                return -1;
            case PAPER:
                if (other == SPOCK || other == ROCK)
                    return 1;
                return -1;
            case SCISSORS:
                if (other == PAPER || other == LIZARD)
                    return 1;
                return -1;
            case SPOCK:
                if (other == ROCK || other == SCISSORS)
                    return 1;
                return -1;
            case LIZARD:
                if (other == SPOCK || other == PAPER)
                    return 1;
                return -1;
            default:
                throw new IllegalArgumentException();
        }
    }
    
    /**
     * Returns a playable's corresponding image's name
     *
     * <pre>{@code String name = Client.Playable.ROCK.imgString();}</pre>
     *
     * @return the name of the image file corresponding to the Client.Playable
     */
    public String imgString() {
        return this.toString().toLowerCase() + ".png";
    }
}