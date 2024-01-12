/**
 * phiola/Android
 * 2022, Simon Zolin
 */

package com.github.stsaz.phiola;

import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.github.stsaz.phiola.databinding.SettingsBinding;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "phiola.SettingsActivity";
    private Core core;
    private SettingsBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = SettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item
                , new String[]{
                "Default",
                "1 (Mono)",
                "2 (Stereo)",
        });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spRecChannels.setAdapter(adapter);

        // Default; 8000..192000 by 1000
        b.sbRecRate.setMax(rec_rate_progress(192000));
        b.sbRecRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    String s = "Default";
                    if (progress != 0)
                        s = core.int_to_str(rec_rate_value(progress));
                    b.eRecRate.setText(s);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item
                , CoreSettings.rec_formats);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spRecEnc.setAdapter(adapter);

        if (Build.VERSION.SDK_INT < 26) {
            b.spRecChannels.setEnabled(false);
            b.sbRecRate.setEnabled(false);
            b.eRecRate.setEnabled(false);
            b.spRecEnc.setEnabled(false);
            b.eRecBufLen.setEnabled(false);
            b.eRecUntil.setEnabled(false);
            b.swRecDanorm.setEnabled(false);
            b.eRecGain.setEnabled(false);
            b.swRecExclusive.setEnabled(false);
        }

        core = Core.getInstance();
        load();
    }

    @Override
    protected void onPause() {
        core.dbglog(TAG, "onPause");
        save();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        core.dbglog(TAG, "onDestroy");
        core.unref();
        super.onDestroy();
    }

    private void load() {
        // Interface
        b.swDark.setChecked(core.gui().theme == GUI.THM_DARK);
        b.swStateHide.setChecked(core.gui().state_hide);
        b.swShowfilter.setChecked(core.gui().filter_hide);
        b.swShowrec.setChecked(core.gui().record_hide);
        b.swSvcNotifDisable.setChecked(core.setts.svc_notification_disable);
        b.swUiInfoInTitle.setChecked(core.gui().ainfo_in_title);

        // Playback
        b.swRandom.setChecked(core.queue().is_random());
        b.swRepeat.setChecked(core.queue().is_repeat());
        b.swNotags.setChecked(core.setts.play_no_tags);
        b.swListAddRmOnNext.setChecked(core.queue().add_rm_on_next);
        b.swListRmOnNext.setChecked(core.queue().rm_on_next);
        b.swListRmOnErr.setChecked(core.queue().rm_on_err);
        b.eCodepage.setText(core.setts.codepage);
        b.eAutoSkip.setText(core.queue().auto_skip_to_str());
        b.eAutoSkipTail.setText(core.queue().auto_skip_tail_to_str());
        b.eAutoStop.setText(core.int_to_str(core.queue().auto_stop_min));

        // Operation
        b.eDataDir.setText(core.setts.pub_data_dir);
        b.eTrashDir.setText(core.setts.trash_dir);
        b.swFileDel.setChecked(core.setts.file_del);
        b.eQuickMoveDir.setText(core.setts.quick_move_dir);

        rec_load();
    }

    private void save() {
        // Interface
        int i = GUI.THM_DEF;
        if (b.swDark.isChecked())
            i = GUI.THM_DARK;
        core.gui().theme = i;

        core.gui().state_hide = b.swStateHide.isChecked();
        core.gui().filter_hide = b.swShowfilter.isChecked();
        core.gui().record_hide = b.swShowrec.isChecked();
        core.gui().ainfo_in_title = b.swUiInfoInTitle.isChecked();

        // Playback
        core.queue().random(b.swRandom.isChecked());
        core.queue().repeat(b.swRepeat.isChecked());
        core.setts.play_no_tags = b.swNotags.isChecked();
        core.queue().add_rm_on_next = b.swListAddRmOnNext.isChecked();
        core.queue().rm_on_next = b.swListRmOnNext.isChecked();
        core.queue().rm_on_err = b.swListRmOnErr.isChecked();
        core.setts.set_codepage(b.eCodepage.getText().toString());
        core.phiola.setCodepage(core.setts.codepage);
        core.queue().auto_skip(b.eAutoSkip.getText().toString());
        core.queue().auto_skip_tail(b.eAutoSkipTail.getText().toString());
        core.queue().auto_stop_min = core.str_to_uint(b.eAutoStop.getText().toString(), 0);

        // Operation
        String s = b.eDataDir.getText().toString();
        if (s.isEmpty())
            s = core.storage_path + "/phiola";
        core.setts.pub_data_dir = s;

        core.setts.svc_notification_disable = b.swSvcNotifDisable.isChecked();
        core.setts.trash_dir = b.eTrashDir.getText().toString();
        core.setts.file_del = b.swFileDel.isChecked();
        core.setts.quick_move_dir = b.eQuickMoveDir.getText().toString();

        rec_save();
        core.queue().conf_normalize();
        core.setts.normalize();
    }

    private int rec_format_index(String s) {
        for (int i = 0; i < CoreSettings.rec_formats.length; i++) {
            if (CoreSettings.rec_formats[i].equals(s))
                return i;
        }
        return -1;
    }

    private static int rec_rate_value(int progress) {
        return 8000 + (progress - 1) * 1000;
    }

    private static int rec_rate_progress(int rate) {
        return 1 + (rate - 8000) / 1000;
    }

    private void rec_load() {
        b.eRecDir.setText(core.setts.rec_path);
        b.spRecChannels.setSelection(core.setts.rec_channels);
        if (core.setts.rec_rate != 0) {
            b.eRecRate.setText(core.int_to_str(core.setts.rec_rate));
            b.sbRecRate.setProgress(rec_rate_progress(core.setts.rec_rate));
        }
        b.spRecEnc.setSelection(rec_format_index(core.setts.rec_enc));
        b.eRecBitrate.setText(Integer.toString(core.setts.rec_bitrate));
        b.eRecBufLen.setText(core.int_to_str(core.setts.rec_buf_len_ms));
        b.eRecUntil.setText(core.int_to_str(core.setts.rec_until_sec));
        b.swRecDanorm.setChecked(core.setts.rec_danorm);
        b.eRecGain.setText(core.float_to_str((float) core.setts.rec_gain_db100 / 100));
        b.swRecExclusive.setChecked(core.setts.rec_exclusive);
    }

    private void rec_save() {
        core.setts.rec_path = b.eRecDir.getText().toString();
        core.setts.rec_bitrate = core.str_to_uint(b.eRecBitrate.getText().toString(), -1);
        core.setts.rec_channels = b.spRecChannels.getSelectedItemPosition();
        core.setts.rec_rate = core.str_to_uint(b.eRecRate.getText().toString(), 0);
        core.setts.rec_enc = CoreSettings.rec_formats[b.spRecEnc.getSelectedItemPosition()];
        core.setts.rec_buf_len_ms = core.str_to_uint(b.eRecBufLen.getText().toString(), -1);
        core.setts.rec_until_sec = core.str_to_uint(b.eRecUntil.getText().toString(), -1);
        core.setts.rec_danorm = b.swRecDanorm.isChecked();
        core.setts.rec_gain_db100 = (int) (core.str_to_float(b.eRecGain.getText().toString(), 0) * 100);
        core.setts.rec_exclusive = b.swRecExclusive.isChecked();
    }
}
