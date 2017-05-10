package saleh.ma.mostafa.gmail.com.simplecalculator.activities;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.codemybrainsout.ratingdialog.RatingDialog;
import com.yqritc.scalablevideoview.ScalableVideoView;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import saleh.ma.mostafa.gmail.com.simplecalculator.R;
import saleh.ma.mostafa.gmail.com.simplecalculator.dialogs.AboutDialog;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "SimpleCalculator";

    static final ButterKnife.Setter<TextView, Boolean> ENABLED = new ButterKnife.Setter<TextView, Boolean>() {
        @Override
        public void set(@NonNull TextView view, Boolean value, int index) {
            view.setEnabled(value);
            if (value) {
                view.setTextColor(Color.BLACK);
                view.setBackgroundResource(R.drawable.function_buttons_selector);
            } else {
                view.setTextColor(Color.LTGRAY);
                view.setBackgroundResource(R.drawable.number_buttons_selector);
            }
        }
    };

    @BindView(R.id.vid_explosion)
    ScalableVideoView vidExplosion;
    @BindView(R.id.scrl_expression)
    HorizontalScrollView scrlExpression;
    @BindView(R.id.tv_expression)
    TextView tvExpression;
    @BindView(R.id.tv_numbers)
    TextView tvNumbers;

    @BindViews({R.id.btn_memory_clear, R.id.btn_memory_recall})
    List<TextView> lstMemoryButtons;

    private boolean mAppend;
    private String mExpression;
    private double mResult;
    private double mMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        ButterKnife.apply(lstMemoryButtons, ENABLED, false);
        mAppend = true;
        mExpression = "";
        new RatingDialog.Builder(this)
                .threshold(3)
                .session(7)
                .onRatingBarFormSumbit(new RatingDialog.Builder.RatingDialogFormListener() {
                    @Override
                    public void onFormSubmitted(String feedback) {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("message/rfc822");
                        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"mostafa.ma.saleh@gmail.com"});
                        i.putExtra(Intent.EXTRA_SUBJECT, "Simple Calculator Feedback");
                        i.putExtra(Intent.EXTRA_TEXT, feedback);
                        try {
                            startActivity(Intent.createChooser(i, "Send mail..."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).build().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            new AboutDialog(this).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.btn_zero, R.id.btn_one, R.id.btn_two,
            R.id.btn_three, R.id.btn_four, R.id.btn_five,
            R.id.btn_six, R.id.btn_seven, R.id.btn_eight,
            R.id.btn_nine})
    public void onNumberClick(TextView v){
        if(mAppend) {
            tvNumbers.setText(String.format(Locale.getDefault(), "%s%s", tvNumbers.getText(), v.getText()));
            if(tvNumbers.getText().charAt(0) == '0' && tvNumbers.getText().charAt(1) != '.')
                tvNumbers.setText(String.format(Locale.getDefault(), "%s", tvNumbers.getText().subSequence(1, tvNumbers.getText().length())));
        } else {
            tvNumbers.setText(v.getText());
            mAppend = true;
        }
    }

    @OnClick({R.id.btn_plus, R.id.btn_minus, R.id.btn_divide, R.id.btn_multiply})
    public void onOperationClick(TextView v){
        if(mAppend) {
            mExpression += tvNumbers.getText().toString() + v.getText();
            mAppend = false;
        } else if (mResult != 0 || !TextUtils.isEmpty(mExpression) && TextUtils.isDigitsOnly(mExpression.substring(mExpression.length()-1))) {
            if (mResult != 0) {
                mExpression += String.valueOf(mResult);
                mResult = 0;
            }
            mExpression += v.getText();
        }
        tvExpression.setText(mExpression);
        scrlExpression.post(new Runnable() {
            @Override
            public void run() {
                scrlExpression.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    @OnClick(R.id.btn_equals)
    public void onEqualsClick(){
        if(mAppend) {
            mExpression += tvNumbers.getText();
            mAppend = false;
        }
        if(!TextUtils.isEmpty(mExpression)) {
            mExpression = mExpression.replaceAll("×","*").replaceAll("÷", "/");
            if (!TextUtils.isDigitsOnly(mExpression.substring(mExpression.length() - 1))) {
                mExpression = mExpression.substring(0, mExpression.length() - 1);
            }
            Expression e = new ExpressionBuilder(mExpression).build();
            if (e.validate() == ValidationResult.SUCCESS) {
                try {
                    mResult = e.evaluate();
                } catch (ArithmeticException ae) {
                    mResult = 0;
                    try {
                        playVideo(vidExplosion);
                    } catch (IOException ioe) {
                        Log.e(TAG, "Failed to initialize video");
                    }
                    Toast.makeText(this, "Seriously, Trying to divide by zero???!!", Toast.LENGTH_LONG).show();
                }
                tvNumbers.setText(String.valueOf(mResult));
                mExpression = "";
                tvExpression.setText("");
            }
        }
    }

    @OnClick(R.id.btn_positive_negative)
    public void onPosNegClick(){
        if(mAppend)
            if(tvNumbers.getText().charAt(0) != '-'){
                tvNumbers.setText(String.format(Locale.getDefault(), "-%s", tvNumbers.getText()));
            } else {
                tvNumbers.setText(String.format(Locale.getDefault(), "%s", tvNumbers.getText().subSequence(1, tvNumbers.getText().length())));
            }
    }

    @OnClick(R.id.btn_decimal_point)
    public void onDecimalPointClick(){
        if(!tvNumbers.getText().toString().contains(".") && mAppend) {
            tvNumbers.setText(String.format(Locale.getDefault(), "%s.", tvNumbers.getText()));
        } else if (mResult == 0){
            tvNumbers.setText("0.");
            mAppend = true;
        }
    }

    @OnClick(R.id.btn_backspace)
    public void onBackspaceClick(){
        if(tvNumbers.getText().length() != 1 && mAppend){
            tvNumbers.setText(String.format(Locale.getDefault(), "%s", tvNumbers.getText().subSequence(0, tvNumbers.getText().length() - 1)));
        } else {
            tvNumbers.setText("0");
        }
    }

    @OnClick(R.id.btn_clear)
    public void onClearClick(){
        tvNumbers.setText("0");
        mAppend = false;
    }

    @OnClick(R.id.btn_clear_all)
    public void onClearAllClick(){
        mExpression = "";
        mResult = 0;
        tvExpression.setText("");
        tvNumbers.setText("0");
        mAppend = false;
    }

    @OnClick(R.id.btn_memory_clear)
    public void onMemoryClearClick() {
        mMemory = 0;
        ButterKnife.apply(lstMemoryButtons, ENABLED, false);
    }

    @OnClick(R.id.btn_memory_recall)
    public void onMemoryRecallClick() {
        tvNumbers.setText(String.valueOf(mMemory));
        mAppend = false;
    }

    @OnClick(R.id.btn_memory_add)
    public void onMemoryAddClick() {
        mMemory += Double.parseDouble(tvNumbers.getText().toString());
        ButterKnife.apply(lstMemoryButtons, ENABLED, true);
    }

    @OnClick(R.id.btn_memory_subtract)
    public void onMemorySubtractClick() {
        mMemory -= Double.parseDouble(tvNumbers.getText().toString());
        ButterKnife.apply(lstMemoryButtons, ENABLED, true);
    }

    @OnClick(R.id.btn_memory_store)
    public void onMemoryStoreClick() {
        mMemory = Double.parseDouble(tvNumbers.getText().toString());
        ButterKnife.apply(lstMemoryButtons, ENABLED, true);
    }

    private void playVideo(final ScalableVideoView vid) throws IOException {
        vid.setRawData(R.raw.explosion);
        vid.prepare();
        vid.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                vid.setVisibility(View.GONE);
            }
        });
        vid.setVisibility(View.VISIBLE);
        vid.start();
    }
}
