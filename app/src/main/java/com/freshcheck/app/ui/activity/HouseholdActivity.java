package com.freshcheck.app.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.freshcheck.app.R;
import com.freshcheck.app.database.HouseholdRepository;
import com.freshcheck.app.firebase.FirebaseAuthManager;
import com.freshcheck.app.model.ActivityLog;
import com.freshcheck.app.model.HouseholdGroup;
import com.freshcheck.app.model.HouseholdMember;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class HouseholdActivity extends AppCompatActivity {
    private static final String TAG = "HouseholdActivity";

    private HouseholdRepository householdRepository;
    private FirebaseAuthManager authManager;

    private TextView tvActiveScopeName;
    private MaterialCardView cardInviteInfo;
    private TextView tvInviteCode;
    private TextView tvMembersList;
    private TextView tvActivityStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_household);

        householdRepository = new HouseholdRepository(this);
        authManager = FirebaseAuthManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar_household);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvActiveScopeName = findViewById(R.id.tv_active_scope_name);
        cardInviteInfo = findViewById(R.id.card_invite_info);
        tvInviteCode = findViewById(R.id.tv_invite_code);
        tvMembersList = findViewById(R.id.tv_members_list);
        tvActivityStream = findViewById(R.id.tv_activity_stream);

        findViewById(R.id.btn_switch_personal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                householdRepository.switchToPersonal();
                Toast.makeText(HouseholdActivity.this, "Switched to Personal Pantry", Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });

        findViewById(R.id.btn_switch_household).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<HouseholdGroup> groups = householdRepository.getAllHouseholds();
                if (groups.isEmpty()) {
                    showCreateHouseholdDialog();
                } else {
                    HouseholdGroup first = groups.get(0);
                    householdRepository.setActiveHousehold(first.getHouseholdId(), first.getName());
                    Toast.makeText(HouseholdActivity.this, "Active: " + first.getName(), Toast.LENGTH_SHORT).show();
                    updateUI();
                }
            }
        });

        findViewById(R.id.btn_create_household).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateHouseholdDialog();
            }
        });

        findViewById(R.id.btn_join_household).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showJoinHouseholdDialog();
            }
        });

        findViewById(R.id.btn_share_invite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareInviteCode();
            }
        });

        updateUI();
    }

    private void updateUI() {
        boolean isHousehold = householdRepository.isHouseholdMode();
        String currentName = householdRepository.getActiveHouseholdName();
        tvActiveScopeName.setText(currentName);

        if (isHousehold) {
            cardInviteInfo.setVisibility(View.VISIBLE);
            HouseholdGroup group = householdRepository.getById(householdRepository.getActiveHouseholdId());
            if (group != null) {
                tvInviteCode.setText(group.getInviteCode());
            }

            // Load members
            List<HouseholdMember> members = householdRepository.getMembers(householdRepository.getActiveHouseholdId());
            if (members.isEmpty()) {
                tvMembersList.setText("• " + authManager.getUserDisplayName() + " (Admin)");
            } else {
                StringBuilder sb = new StringBuilder();
                for (HouseholdMember m : members) {
                    sb.append("• ").append(m.getUserName()).append(" (").append(m.getRole()).append(")\n");
                }
                tvMembersList.setText(sb.toString().trim());
            }

            // Load activity logs
            List<ActivityLog> logs = householdRepository.getActivityLogs(householdRepository.getActiveHouseholdId(), 6);
            if (logs.isEmpty()) {
                tvActivityStream.setText("No recent activity recorded.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (ActivityLog l : logs) {
                    sb.append("• ").append(l.getUserName()).append(": ").append(l.getAction())
                            .append(" (").append(l.getFormattedTime()).append(")\n");
                }
                tvActivityStream.setText(sb.toString().trim());
            }
        } else {
            cardInviteInfo.setVisibility(View.GONE);
            tvMembersList.setText("Personal mode active. Connect to a mess to view shared members.");
            tvActivityStream.setText("Switch to a Shared Mess above to view collaborative activities.");
        }
    }

    private void showCreateHouseholdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Shared Household / Mess");

        final EditText etName = new EditText(this);
        etName.setHint("e.g., Room 304 Mess, Podder Residence");
        etName.setPadding(40, 30, 40, 30);
        builder.setView(etName);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) name = "My Shared Mess";

                HouseholdGroup group = householdRepository.createHousehold(
                        name,
                        authManager.getUserId(),
                        authManager.getUserDisplayName()
                );
                Toast.makeText(HouseholdActivity.this, "Mess created! Code: " + group.getInviteCode(), Toast.LENGTH_LONG).show();
                updateUI();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showJoinHouseholdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Join Mess with Invite Code");

        final EditText etCode = new EditText(this);
        etCode.setHint("e.g. MESS-8X2K");
        etCode.setPadding(40, 30, 40, 30);
        builder.setView(etCode);

        builder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String code = etCode.getText().toString().trim().toUpperCase();
                if (code.isEmpty()) {
                    Toast.makeText(HouseholdActivity.this, "Please enter an invite code", Toast.LENGTH_SHORT).show();
                    return;
                }

                HouseholdGroup group = householdRepository.joinHousehold(
                        code,
                        authManager.getUserId(),
                        authManager.getUserDisplayName()
                );
                Toast.makeText(HouseholdActivity.this, "Joined: " + group.getName(), Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void shareInviteCode() {
        String code = tvInviteCode.getText().toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Join my FreshCheck Mess");
        intent.putExtra(Intent.EXTRA_TEXT, "Join our shared pantry and grocery list on FreshCheck using this invite code: " + code);
        startActivity(Intent.createChooser(intent, "Share Invite Code"));
    }
}