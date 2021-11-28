package jp.co.pattirudon.pokemon;

public enum PersonalityMark implements Mark {
    Rowdy(0), AbsentMinded(1), Jittery(2), Excited(3), Charismatic(4), Calmness(5), Intense(6), ZonedOut(7), Joyful(8),
    Angry(9), Smiley(10), Teary(11), Upbeat(12), Peeved(13), Intellectual(14), Ferocious(15), Crafty(16), Scowling(17),
    Kindly(18), Flustered(19), PumpedUp(20), ZeroEnergy(21), Prideful(22), Unsure(23), Humble(24), Thorny(25),
    Vigor(26), Slump(27);

    final int id;

    PersonalityMark(int id) {
        this.id = id;
    }

    public static PersonalityMark valueOf(int id) {
        return PersonalityMark.values()[id];
    }
}
