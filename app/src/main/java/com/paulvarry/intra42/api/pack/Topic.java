package com.paulvarry.intra42.api.pack;

import androidx.annotation.Nullable;
import com.paulvarry.intra42.R;
import com.paulvarry.intra42.activities.TopicActivity;
import com.paulvarry.intra42.api.ApiService;
import com.paulvarry.intra42.api.model.Messages;
import com.paulvarry.intra42.api.model.Topics;
import com.paulvarry.intra42.ui.BasicThreadActivity;
import com.paulvarry.intra42.utils.Tools;
import retrofit2.Call;

import java.io.IOException;
import java.util.List;

public class Topic {

    public Topics topic;
    public List<Messages> messages;

    @Nullable
    static public Topic get(TopicActivity activity, ApiService service, int id) throws IOException, BasicThreadActivity.UnauthorizedException, BasicThreadActivity.ErrorServerException {
        Topic topic = new Topic();

        Call<Topics> callTopic = service.getTopic(id);
        Call<List<Messages>> callMessages = service.getTopicMessages(id);

        activity.setLoadingProgress(activity.getString(R.string.info_loading_topic), 1, 3);
        retrofit2.Response<Topics> retTopic = callTopic.execute();
        if (!Tools.apiIsSuccessful(retTopic))
            return null;

        activity.setLoadingProgress(R.string.info_loading_topic_reply, 2, 3);
        retrofit2.Response<List<Messages>> retMessages = callMessages.execute();
        if (!Tools.apiIsSuccessful(retMessages))
            return null;

        activity.setLoadingProgress(3, 3);
        topic.topic = retTopic.body();
        topic.messages = retMessages.body();

        return topic;
    }

    @Nullable
    static public Topic get(TopicActivity activity, ApiService service, Topics topics) {
        Topic topic = new Topic();

        Call<List<Messages>> callMessages = service.getTopicMessages(topics.id);
        try {
            activity.setLoadingProgress(R.string.info_loading_topic_reply);
            retrofit2.Response<List<Messages>> retMessages = callMessages.execute();
            if (!(retMessages.code() == 200))
                return null;
            topic.topic = topics;
            topic.messages = retMessages.body();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return topic;
    }
}
