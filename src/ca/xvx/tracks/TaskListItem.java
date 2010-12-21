package ca.xvx.tracks;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class TaskListItem extends RelativeLayout {
	private TextView _name;
	private TextView _info;
	private CheckBox _done;

	private Task _task;

	public TaskListItem(Context c, Task t) {
		super(c);

		if(t == null) {
			return;
		}
		_task = t;

		addView(inflate(c, R.layout.tasklist_item, null));
		_name = (TextView)findViewById(R.id.TLI_name);
		_info = (TextView)findViewById(R.id.TLI_info);
		_done = (CheckBox)findViewById(R.id.TLI_done);

		_name.setText(_task.getDescription());
		String infos = "";
		if(t.getProject() != null) {
			infos += "Project: " + t.getProject().getName();
		}
		_info.setText(infos);

		_done.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton b, boolean checked) {
					_task.setDone(checked);
				}
			});
	}

	protected Task getTask() {
		return _task;
	}
}
