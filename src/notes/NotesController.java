/*
 * Copyright 2017 DSATool team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package notes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.javafx.scene.web.skin.HTMLEditorSkin;

import dsatool.resources.ResourceManager;
import dsatool.util.ErrorLogger;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import jsonant.event.JSONListener;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class NotesController implements JSONListener {

	private String currentText;

	/**
	 * Contains the notes data
	 */
	private JSONArray data;

	@FXML
	private ListView<String> list;

	private MultipleSelectionModel<String> listModel;

	@FXML
	private HTMLEditor text;

	@FXML
	private TextField title;

	private Node pane;

	public NotesController() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			pane = fxmlLoader.load(getClass().getResource("Notes.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		prepare();

		for (final Node n : ((GridPane) ((HTMLEditorSkin) text.getSkin()).getChildren().get(0)).getChildren()) {
			n.setOnMouseExited(e -> textChanged());
			n.focusedProperty().addListener((o, oldV, newV) -> {
				if (!newV) {
					textChanged();
				}
			});
		}
	}

	private void addEmpty() {
		final JSONObject notizen = ResourceManager.getResource("Notizen");
		final JSONArray daten = notizen.getArr("Notizen");
		final JSONArray neu = new JSONArray(daten);
		neu.add("Unbenannt");
		neu.add("");
		daten.add(neu);
		daten.notifyListeners(this);
		list.getItems().add("Unbenannt");
		listModel.clearAndSelect(list.getItems().size() - 1);
		title.setText("Unbenannt");
		text.setHtmlText("");
	}

	@FXML
	private void addEntry() {
		addEmpty();
	}

	public Node getControl() {
		return pane;
	}

	public void load() {
		final JSONObject notizen = ResourceManager.getResource("Notizen");
		data = notizen.getArr("Notizen");
		if (data == null) {
			data = new JSONArray(notizen);
			notizen.put("Notizen", data);
			notizen.notifyListeners(this);
		}
		data.addListener(this);

		reload();
	}

	@FXML
	private void loadFile() {
		final FileChooser dialog = new FileChooser();

		dialog.setTitle("Datei öffnen");
		dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.*", "*.*"));

		final File file = dialog.showOpenDialog(null);
		if (file != null) {
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				final JSONObject notizen = ResourceManager.getResource("Notizen");
				final JSONArray daten = notizen.getArr("Notizen");
				final JSONArray neu = new JSONArray(daten);
				String name = file.getName();
				name = name.substring(0, name.indexOf('.'));
				neu.add(name);
				final StringBuilder content = new StringBuilder();
				String currentLine;
				while ((currentLine = reader.readLine()) != null) {
					content.append(currentLine).append('\n');
				}
				neu.add(content.toString());
				daten.add(neu);
				daten.notifyListeners(this);
				list.getItems().add(name);
				listModel.clearAndSelect(list.getItems().size() - 1);
				title.setText(daten.getArr(listModel.getSelectedIndex()).getString(0));
				text.setHtmlText(daten.getArr(listModel.getSelectedIndex()).getString(1));
			} catch (final IOException e1) {
				ErrorLogger.logError(e1);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see resources.ResourceListener#notifyChanged()
	 */
	@Override
	public void notifyChanged(JSONValue changed) {
		reload();
	}

	private void prepare() {
		listModel = list.getSelectionModel();

		listModel.selectedIndexProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
			if (listModel.getSelectedIndex() > -1) {
				final JSONObject notizen = ResourceManager.getResource("Notizen");
				final JSONArray daten = notizen.getArr("Notizen");
				title.setText(daten.getArr(listModel.getSelectedIndex()).getString(0));
				text.setHtmlText(daten.getArr(listModel.getSelectedIndex()).getString(1));
			}
		});

		load();
	}

	/**
	 * Reloads the data if it has changed
	 */
	private void reload() {
		int selected = Math.min(data.size() - 1, Math.max(0, listModel.getSelectedIndex()));
		list.getItems().clear();
		if (data.size() == 0) {
			final JSONArray neu = new JSONArray(data);
			neu.add("Unbenannt");
			neu.add("");
			data.add(neu);
			data.notifyListeners(this);
			selected = 0;
		}
		for (int i = 0; i < data.size(); ++i) {
			list.getItems().add(data.getArr(i).getString(0));
		}
		title.setText(data.getArr(selected).getString(0));
		text.setHtmlText(data.getArr(selected).getString(1));
		listModel.clearAndSelect(selected);
	}

	@FXML
	private void removeEntry() {
		final JSONObject notizen = ResourceManager.getResource("Notizen");
		final JSONArray daten = notizen.getArr("Notizen");
		final int index = listModel.getSelectedIndex();
		daten.removeAt(index);
		daten.notifyListeners(this);
		list.getItems().remove(index);
		if (daten.size() == 0) {
			addEmpty();
			return;
		} else if (index == daten.size()) {
			listModel.selectLast();
		} else {
			listModel.clearAndSelect(index);
		}
		title.setText(daten.getArr(listModel.getSelectedIndex()).getString(0));
		text.setHtmlText(daten.getArr(listModel.getSelectedIndex()).getString(1));
	}

	@FXML
	private void saveFile() {
		if (listModel.getSelectedIndex() > -1) {
			final FileChooser dialog = new FileChooser();

			dialog.setTitle("Datei speichern");
			dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.*", "*.*"));

			final File file = dialog.showSaveDialog(null);
			if (file != null) {
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
					writer.write(text.getHtmlText());
				} catch (final IOException e1) {
					ErrorLogger.logError(e1);
				}
			}
		}
	}

	@FXML
	private void textChanged() {
		if (listModel.getSelectedIndex() > -1) {
			if (!text.getHtmlText().equals(currentText)) {
				currentText = text.getHtmlText();
				final JSONObject notizen = ResourceManager.getResource("Notizen");
				final JSONArray daten = notizen.getArr("Notizen");
				daten.getArr(listModel.getSelectedIndex()).set(1, currentText);
				daten.notifyListeners(this);
			}
		}
	}

	@FXML
	private void titleChanged() {
		if (listModel.getSelectedIndex() > -1) {
			final JSONObject notizen = ResourceManager.getResource("Notizen");
			final JSONArray daten = notizen.getArr("Notizen");
			final int selected = listModel.getSelectedIndex();
			daten.getArr(selected).set(0, title.getText());
			final int caret = title.getCaretPosition();
			final IndexRange selection = title.getSelection();
			list.getItems().set(selected, title.getText());
			listModel.clearAndSelect(selected);
			if (selection.getStart() == caret) {
				title.selectRange(selection.getEnd(), selection.getStart());
			} else {
				title.selectRange(selection.getStart(), selection.getEnd());
			}
			daten.notifyListeners(this);
		}
	}

}
