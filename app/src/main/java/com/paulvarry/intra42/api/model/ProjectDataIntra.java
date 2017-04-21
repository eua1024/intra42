package com.paulvarry.intra42.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProjectDataIntra {

    private static final String API_STATE = "state";
    private static final String API_FINAL_MARK = "final_mark";
    private static final String API_ID = "id";
    private static final String API_KIND = "kind";
    private static final String API_NAME = "name";
    private static final String API_X = "x";
    private static final String API_Y = "y";
    private static final String API_BY = "by";
    private static final String API_PROJECT_ID = "project_id";
    private static final String API_DIFFICULTY = "difficulty";
    private static final String API_DURATION = "duration";
    private static final String API_RULES = "rules";
    private static final String API_DESCRIPTION = "description";
    private static final String API_SLUG = "slug";

    @SerializedName(API_STATE)
    public State state;
    @SerializedName(API_FINAL_MARK)
    public Integer finalMark;
    @SerializedName(API_ID)
    public int id;
    @SerializedName(API_KIND)
    public Kind kind;
    @SerializedName(API_NAME)
    public String name;
    @SerializedName(API_X)
    public int x;
    @SerializedName(API_Y)
    public int y;
    @SerializedName(API_BY)
    public List<By> by = null;
    @SerializedName(API_PROJECT_ID)
    public int projectId;
    @SerializedName(API_DIFFICULTY)
    @Expose
    public String difficulty;
    @SerializedName(API_DURATION)
    @Expose
    public String duration;
    @SerializedName(API_RULES)
    @Expose
    public String rules;
    @SerializedName(API_DESCRIPTION)
    @Expose
    public String description;
    @SerializedName(API_SLUG)
    @Expose
    public String slug;

    public enum Kind {
        @SerializedName("project")PROJECT,
        @SerializedName("big_project")BIG_PROJECT,
        @SerializedName("piscine")PISCINE,
        @SerializedName("rush")RUSH,
        @SerializedName("part_time")PART_TIME,
        @SerializedName("first_internship")FIRST_INTERNSHIP,
        @SerializedName("second_internship")SECOND_INTERNSHIP,
        @SerializedName("exam")EXAM
    }

    public enum State {
        @SerializedName("done")DONE,
        @SerializedName("available")AVAILABLE,
        @SerializedName("in_progress")IN_PROGRESS,
        @SerializedName("unavailable")UNAVAILABLE
    }

    public class By {

        @SerializedName("parent_id")
        public int parentId;
        @SerializedName("points")
        public List<List<Integer>> points = null;

    }

}