package com.paste.android;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class ManageDestinationsDialog {

    public interface OnDestinationsChangedListener {
        void onDestinationsChanged();
    }

    public static void show(Context context, SettingsManager settingsManager, OnDestinationsChangedListener listener) {
        List<LinuxDestination> destinations = settingsManager.getDestinations();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_manage_destinations, null);
        ListView listView = dialogView.findViewById(R.id.destinationsListView);
        Button addButton = dialogView.findViewById(R.id.addDestinationButton);

        DestinationAdapter adapter = new DestinationAdapter(context, destinations, settingsManager, listener);
        listView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Manage Destinations")
                .setView(dialogView)
                .setPositiveButton("Done", null)
                .create();

        addButton.setOnClickListener(v -> {
            DestinationDialog.showAddDestinationDialog(context, destination -> {
                destinations.add(destination);
                settingsManager.saveDestinations(destinations);
                adapter.notifyDataSetChanged();
                listener.onDestinationsChanged();
                Toast.makeText(context, "Destination added", Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    private static class DestinationAdapter extends ArrayAdapter<LinuxDestination> {
        private final SettingsManager settingsManager;
        private final OnDestinationsChangedListener listener;

        public DestinationAdapter(Context context, List<LinuxDestination> destinations, 
                                 SettingsManager settingsManager, OnDestinationsChangedListener listener) {
            super(context, 0, destinations);
            this.settingsManager = settingsManager;
            this.listener = listener;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_destination, parent, false);
            }

            LinuxDestination destination = getItem(position);
            TextView nameText = convertView.findViewById(R.id.destinationNameText);
            TextView detailsText = convertView.findViewById(R.id.destinationDetailsText);
            Button editButton = convertView.findViewById(R.id.editDestinationButton);
            Button deleteButton = convertView.findViewById(R.id.deleteDestinationButton);

            nameText.setText(destination.getName());
            detailsText.setText(destination.getUser() + "@" + destination.getHost() + ":" + 
                               destination.getPort() + " → " + destination.getDirectory());

            editButton.setOnClickListener(v -> {
                DestinationDialog.showEditDestinationDialog(getContext(), destination, editedDestination -> {
                    destination.setName(editedDestination.getName());
                    destination.setUser(editedDestination.getUser());
                    destination.setHost(editedDestination.getHost());
                    destination.setPort(editedDestination.getPort());
                    destination.setDirectory(editedDestination.getDirectory());
                    settingsManager.saveDestinations((List<LinuxDestination>) this.getAll());
                    notifyDataSetChanged();
                    listener.onDestinationsChanged();
                    Toast.makeText(getContext(), "Destination updated", Toast.LENGTH_SHORT).show();
                });
            });

            deleteButton.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Destination")
                        .setMessage("Delete " + destination.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            remove(destination);
                            settingsManager.saveDestinations((List<LinuxDestination>) this.getAll());
                            notifyDataSetChanged();
                            listener.onDestinationsChanged();
                            Toast.makeText(getContext(), "Destination deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            return convertView;
        }

        private Iterable<LinuxDestination> getAll() {
            List<LinuxDestination> all = new java.util.ArrayList<>();
            for (int i = 0; i < getCount(); i++) {
                all.add(getItem(i));
            }
            return all;
        }
    }
}
