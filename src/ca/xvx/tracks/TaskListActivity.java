package ca.xvx.tracks;

import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.Toast;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class TaskListActivity extends ExpandableListActivity {
	private TaskListAdapter _tla;
	private SharedPreferences _prefs;
	private Handler _commHandler;

	private static final int INIT_SETTINGS = 1;
	private static final int NEW_TASK = 2;
	private static final int EDIT_TASK = 2;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		_prefs = PreferenceManager.getDefaultSharedPreferences(this);

		TracksCommunicator comm = new TracksCommunicator(_prefs);
		comm.start();
		_commHandler = comm.getHandler();
		
		_tla = new TaskListAdapter();
		setListAdapter(_tla);
		if(!_prefs.getBoolean(PreferenceConstants.RUN, false)) {
			_prefs.edit().putBoolean(PreferenceConstants.RUN, true).commit();
			startActivityForResult(new Intent(this, SettingsActivity.class), INIT_SETTINGS);
		} else {
			refreshList();
		}
	}

	private void refreshList() {
		final Context context = getExpandableListView().getContext();
		final ProgressDialog p = ProgressDialog.show(context, "", "", true);
		TracksAction a = new TracksAction(TracksAction.ActionType.FETCH_TASKS, null, new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch(msg.what) {
					case TracksCommunicator.FETCH_CODE:
						p.setMessage(getString(R.string.MSG_fetching));
						break;
					case TracksCommunicator.PARSE_CODE:
						p.setMessage(getString(R.string.MSG_parsing));
						break;
					case TracksCommunicator.PREFS_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_badprefs), Toast.LENGTH_LONG).show();
						break;
					case TracksCommunicator.PARSE_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_parse), Toast.LENGTH_LONG).show();
						break;
					case TracksCommunicator.FETCH_FAIL_CODE:
						p.dismiss();
						Toast.makeText(context, getString(R.string.ERR_fetch), Toast.LENGTH_LONG).show();
						break;
					case TracksCommunicator.SUCCESS_CODE:
						_tla.notifyDataSetChanged();
						p.dismiss();

						int ngroups = _tla.getGroupCount();
						for(int g = 0; g < ngroups; g++) {
							if(!((TodoContext)_tla.getGroup(g)).isHidden()) {
								getExpandableListView().expandGroup(g);
							}
						}

						for(Task t : Task.getAllTasks()) {
							t.setChangeHandler(_commHandler, _tla.getNotifyHandler());
						}
						
						registerForContextMenu(getExpandableListView());
						break;
					}
				}
			});
		
		Message.obtain(_commHandler, 0, a).sendToTarget();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)menuInfo;
		if(info.targetView instanceof TaskListItem) {
			MenuInflater inflater = getMenuInflater();
			TaskListItem tli = (TaskListItem)info.targetView;
			inflater.inflate(R.menu.task_context_menu, menu);
			menu.setHeaderTitle(tli.getTask().getDescription());
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
		int cid = getExpandableListView().getPackedPositionChild(info.packedPosition);
		int gid = getExpandableListView().getPackedPositionGroup(info.packedPosition);
		Task t = (Task)_tla.getChild(gid, cid);
		String desc = t.getDescription();
		Context context = getExpandableListView().getContext();
		
		switch(item.getItemId()) {
		case R.id.edit_task:
			Intent i = new Intent(this, TaskEditorActivity.class);
			i.putExtra("task", t.getId());
			startActivityForResult(i, EDIT_TASK);
			return true;
		case R.id.delete_task:
			Toast.makeText(context, "I would delete " + desc, Toast.LENGTH_LONG).show();
			return true;
		case R.id.done_task:
			t.setDone(true);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == INIT_SETTINGS) {
			refreshList();
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView l, View view, int group, int position, long id) {
		Task t = (Task)_tla.getChild(group, position);

		Intent i = new Intent(this, TaskEditorActivity.class);
		i.putExtra("task", t.getId());
		startActivityForResult(i, EDIT_TASK);
		
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inf = getMenuInflater();
		inf.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.MENU_add:
			startActivityForResult(new Intent(this, TaskEditorActivity.class), NEW_TASK);
			return true;
		case R.id.MENU_settings:
			startActivityForResult(new Intent(this, SettingsActivity.class), INIT_SETTINGS);
			return true;
		case R.id.MENU_refresh:
			refreshList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
