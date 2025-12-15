package com.example.speakup.Objects;

public class Question {
    private String questionId;
    private String category;
    private String topic;
    private String subTopic;
    private String fullQuestion;
    private String briefQuestion;
    private String videoUrl;

    public Question(String category, String topic, String subTopic, String fullQuestion, String briefQuestion, String videoUrl) {
        this.category = category;
        this.topic = topic;
        this.subTopic = subTopic;
        this.fullQuestion = fullQuestion;
        this.briefQuestion = briefQuestion;
        if (this.category.equals("Video Clip Questions")) {
            this.videoUrl = videoUrl;
        } else {
            this.videoUrl = "null";
        }
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getCategory() {
        return category;
    }

    public String getTopic() {
        return topic;
    }

    public String getSubTopic() {
        return subTopic;
    }

    public String getFullQuestion() {
        return fullQuestion;
    }

    public String getBriefQuestion() {
        return briefQuestion;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public Question() {}
}
