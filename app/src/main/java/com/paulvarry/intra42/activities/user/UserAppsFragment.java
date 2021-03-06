package com.paulvarry.intra42.activities.user;

import android.content.Intent;
import android.net.Uri;
import android.webkit.URLUtil;
import androidx.annotation.Nullable;
import com.paulvarry.intra42.adapters.ListAdapterApps;
import com.paulvarry.intra42.api.ApiService;
import com.paulvarry.intra42.api.model.Apps;
import com.paulvarry.intra42.ui.BasicFragmentCall;
import retrofit2.Call;

import java.util.List;

public class UserAppsFragment extends BasicFragmentCall<Apps, ListAdapterApps> {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment UserPartnershipsFragment.
     */
    public static UserAppsFragment newInstance() {
        return new UserAppsFragment();
    }

    @Nullable
    @Override
    public Call<List<Apps>> getCall(ApiService apiService, int page) {

        UserActivity activity = (UserActivity) getActivity();
        if (activity != null)
            return apiService.getUsersApps(activity.login);
        return null;
    }

    @Override
    public String getEmptyMessage() {
        return null;
    }

    @Override
    public ListAdapterApps generateAdapter(List<Apps> list) {
        return new ListAdapterApps(getContext(), list);
    }

    @Override
    public void onItemClick(Apps item) {
        if (item.website != null && !item.website.isEmpty() && URLUtil.isValidUrl(item.website)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(item.website));
            getActivity().startActivity(intent);
        }
    }
}
