package com.example.mediaplayer;

public class Utilities {

    public String milliSecondsToTimer(long milliseconds) {
        String finalTimerString;

        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        finalTimerString = convertToString(hours) + ":" + convertToString(minutes) + ":" + convertToString(seconds);
        return finalTimerString;
    }

    public int getProgressPercentage(long currentDuration, long totalDuration) {
        double percentage;
        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);

        percentage = (((double) currentSeconds) / totalSeconds) * 100;
        return (int) percentage;
    }

    public int progressToTimer(int progress, int totalDuration) {
        int currentDuration;
        totalDuration /= 1000;
        currentDuration = (int) ((((double) progress) / 100) * totalDuration);
        return currentDuration * 1000;
    }

    /**
     * Prepending 0 if it is one digit
     */
    public String convertToString(int duration) {
        return duration < 10 ? "0" + duration : "" + duration;
    }
}
