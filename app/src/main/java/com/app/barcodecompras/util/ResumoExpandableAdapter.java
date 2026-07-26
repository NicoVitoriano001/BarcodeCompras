package com.app.barcodecompras.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class ResumoExpandableAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> groups;
    private Map<String, String> children;

    public ResumoExpandableAdapter(Context context, List<String> groups, Map<String, String> children) {
        this.context = context;
        this.groups = groups;
        this.children = children;
    }

    @Override
    public int getGroupCount() { return groups.size(); }

    @Override
    public int getChildrenCount(int groupPosition) { return 1; }

    @Override
    public Object getGroup(int groupPosition) { return groups.get(groupPosition); }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return children.get(groups.get(groupPosition));
    }
    @Override
    public long getGroupId(int i) { return i; }

    @Override
    public long getChildId(int i, int i1) { return i1; }

    @Override
    public boolean hasStableIds() { return false; }
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        TextView tv = new TextView(context);
        tv.setText(groups.get(groupPosition));
        tv.setTextSize(16);  // ✅ pai maior
        tv.setPadding(40, 20, 20, 20);

        return tv;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {

        TextView tv = new TextView(context);
        tv.setText(children.get(groups.get(groupPosition)));
        tv.setTextSize(13); // ✅ menor (como você pediu)
        tv.setPadding(80, 20, 20, 20);

        return tv;
    }
    @Override
    public boolean isChildSelectable(int i, int i1) { return false; }

}
