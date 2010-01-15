package org.liberty.android.fantasisichmemo;

import java.util.ArrayList;
import java.util.ListIterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MemoScreen extends Activity {
	private ArrayList<Item> acqQueue;
	private ArrayList<Item> revQueue;
	private DatabaseHelper dbHelper;
	private String dbName;
	private String dbPath;
	private String mode;
	private boolean showAnswer;
	private int newGrade = -1;
	private Item currentItem;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.memo_screen);
		
		// The extra mode field is passed from intent.
		// acq and rev should be different processes in different learning algorithm
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mode = extras.getString("mode");
			if (!mode.equals("acq") && !mode.equals("rev")) {
				mode = null;
			}
			dbPath = extras.getString("dbpath");
			dbName = extras.getString("dbname");
		}
		this.prepare();
		this.feedData();
		if(acqQueue.isEmpty() && revQueue.isEmpty()){
			OnClickListener backButtonListener = new OnClickListener() {
				// Finish the current activity and go back to the last activity.
				// It should be the main screen.
				public void onClick(DialogInterface arg0, int arg1) {
					finish();
				}
			};
			AlertDialog alertDialog = new AlertDialog.Builder(this)
			.create();
			alertDialog.setTitle("No item");
			alertDialog.setMessage("There is no "+ mode + "items for now.");
			alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Back",
					backButtonListener);
			alertDialog.show();
			
		}
		else{
			this.updateMemoScreen();
		}

	}

	public boolean onTouchEvent(MotionEvent event) {
		// When the screen is touched, it will uncover answer
		int eventAction = event.getAction();
		switch (eventAction) {
		case MotionEvent.ACTION_DOWN:
			this.showAnswer ^= true;
			updateMemoScreen();

		}
		return true;

	}

	private void prepare() {
		// Empty the queue, init the db
		dbHelper = new DatabaseHelper(this, dbPath, dbName);
		acqQueue = new ArrayList<Item>();
		revQueue = new ArrayList<Item>();
		this.newGrade = -1;
	}

	private void feedData() {
		// Feed the 10 items to acq queue
		// or feed all items to rev queue
		// from the database
		if (mode.equals("acq")) {
			this.feedDataFromAcq(10);
			if (acqQueue.isEmpty()) {
				currentItem = null;
			} else {
				// We set the currerntItem to the first item in queue
				currentItem = acqQueue.get(0);
			}
		} else if (mode.equals("rev")) {
			this.feedDataFromRev(0);
			if (revQueue.isEmpty()) {
				currentItem = null;
			} else {
				currentItem = revQueue.get(0);
			}

		}
	}

	private void updateMemoScreen() {
		// update the main screen according to the currentItem
		
		OnClickListener backButtonListener = new OnClickListener() {
			// Finish the current activity and go back to the last activity.
			// It should be the main screen.
			public void onClick(DialogInterface arg0, int arg1) {
				finish();
			}
		};
		if (mode.equals("acq")) {
				// When the acq queue is empty we try to feed data again
				if (acqQueue.isEmpty()) {
					prepare();
					feedData();
					if(!acqQueue.isEmpty()){
					// If the data is successfully fed, we ask user if 
					// he/she want to continue learning
					
					OnClickListener yesButtonListener = new OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							updateMemoScreen();

						}
					};
					OnClickListener noButtonListener = new OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {

						}
					};
					AlertDialog alertDialog = new AlertDialog.Builder(this)
							.create();
					alertDialog.setTitle("Congratulations!");
					alertDialog
							.setMessage("You have learned 10 items. Do you want to learn more?");
					alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
							yesButtonListener);
					alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
							noButtonListener);
					alertDialog.show();
				} else {
					// If the queue is still empty, there is no new items in database.
					// So we ask user to go back the main screen.
					
					AlertDialog alertDialog = new AlertDialog.Builder(this)
							.create();
					alertDialog.setTitle("No Data in Database");
					alertDialog
							.setMessage("You have learned all the new items in database");
					alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Back",
							backButtonListener);
					alertDialog.show();

				}
			}

			 else {
				currentItem = acqQueue.get(0);
				this.displayQA(currentItem);
			}
		} else if (mode.equals("rev")) {
			// The revision process is different. It ask user to review all items
			// So if there is no items in the queue, there shouldn't be any items
			// for revision in the database.
			// We can only ask user to go back.
			
				if (revQueue.isEmpty()) {
					AlertDialog alertDialog = new AlertDialog.Builder(this)
							.create();
					alertDialog.setTitle("Congratulations!");
					alertDialog.setMessage("You have completed revision!");
					alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Back",
							backButtonListener);
					alertDialog.show();
				}

			 else {
				currentItem = revQueue.get(0);
				this.displayQA(currentItem);
			}
		}
	}

	private void feedDataFromAcq(int numberOfItems) {
		// Feed n items from the acq queue.
		Item item;
		int id = -1;
		if (numberOfItems == 0) {
			numberOfItems = 65536;
		}
		for (int i = 0; i < numberOfItems; i++) {
			item = dbHelper.getItemById(id, 1);
			if (item == null) {
				break;
			}
			acqQueue.add(item);
			id = item.getId() + 1;

		}
	}

	private void feedDataFromRev(int numberOfItems) {
		// Feed n items from rev queue
		Item item;
		int id = -1;
		if (numberOfItems == 0) {
			numberOfItems = 65536;
		}
		for (int i = 0; i < numberOfItems; i++) {
			item = dbHelper.getItemById(id, 2);
			if (item == null) {
				break;
			}
			revQueue.add(item);
			id = item.getId() + 1;
		}
	}

	private void displayQA(Item item) {
		// Display question and answer according to item
		TextView questionView = (TextView) findViewById(R.id.question);
		TextView answerView = (TextView) findViewById(R.id.answer);
		questionView.setText(new StringBuilder().append(item.getQuestion()));
		answerView.setText(new StringBuilder().append(item.getAnswer()));
		this.buttonBinding();

	}

	private void clickHandling() {
		
		// When user click on the button of grade, it will update the item information
		// according to the grade.
		// If the return value is success, the user will not need to see this item today.
		// If the return value is failure, the item will be appended to the tail of the queue.

		boolean success = currentItem.processAnswer(newGrade);
		if (mode.equals("acq")) {
			if (success == true) {
				acqQueue.remove(0);
				dbHelper.updateItem(currentItem);
			} else {
				acqQueue.remove(0);
				acqQueue.add(currentItem);
			}

		} else if (mode.equals("rev")) {
			if (success == true) {
				revQueue.remove(0);
				dbHelper.updateItem(currentItem);
			} else {
				revQueue.remove(0);
				revQueue.add(currentItem);
			}
		}
		this.showAnswer = false;
		// Now the currentItem is the next item, so we need to udpate the screen.
		
		this.updateMemoScreen();
	}

	private void buttonBinding() {
		// This function will bind the button event and show/hide button
		// according to the showAnswer varible.
		Button btn0 = (Button) findViewById(R.id.But00);
		Button btn1 = (Button) findViewById(R.id.But01);
		Button btn2 = (Button) findViewById(R.id.But02);
		Button btn3 = (Button) findViewById(R.id.But03);
		Button btn4 = (Button) findViewById(R.id.But04);
		Button btn5 = (Button) findViewById(R.id.But05);
		TextView answer = (TextView) findViewById(R.id.answer);
		if (showAnswer == false) {
			btn0.setVisibility(View.INVISIBLE);
			btn1.setVisibility(View.INVISIBLE);
			btn2.setVisibility(View.INVISIBLE);
			btn3.setVisibility(View.INVISIBLE);
			btn4.setVisibility(View.INVISIBLE);
			btn5.setVisibility(View.INVISIBLE);
			answer.setText(new StringBuilder().append("?\n Show answer"));

		} else {
			View.OnClickListener btn0Listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					newGrade = 0;
					clickHandling();
				}
			};
			View.OnClickListener btn1Listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					newGrade = 1;
					clickHandling();
				}
			};
			View.OnClickListener btn2Listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					newGrade = 2;
					clickHandling();
				}
			};
			View.OnClickListener btn3Listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					newGrade = 3;
					clickHandling();
				}
			};
			View.OnClickListener btn4Listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					newGrade = 4;
					clickHandling();
				}
			};
			View.OnClickListener btn5Listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					newGrade = 5;
					clickHandling();
				}
			};
			btn0.setVisibility(View.VISIBLE);
			btn1.setVisibility(View.VISIBLE);
			btn2.setVisibility(View.VISIBLE);
			btn3.setVisibility(View.VISIBLE);
			btn4.setVisibility(View.VISIBLE);
			btn5.setVisibility(View.VISIBLE);
			btn0.setOnClickListener(btn0Listener);
			btn1.setOnClickListener(btn1Listener);
			btn2.setOnClickListener(btn2Listener);
			btn3.setOnClickListener(btn3Listener);
			btn4.setOnClickListener(btn4Listener);
			btn5.setOnClickListener(btn5Listener);

		}
	}

}
