package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.gui.maintable.columns.FileColumn;
import org.jabref.gui.maintable.columns.LibraryColumn;
import org.jabref.gui.maintable.columns.LinkedIdentifierColumn;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.MatchCategory;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.groups.AbstractGroup;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTableColumnFactory {

    public static final String STYLE_ICON_COLUMN = "column-icon";
    private static final Logger LOGGER = LoggerFactory.getLogger(MainTableColumnFactory.class);

    private final GuiPreferences preferences;
    private final ColumnPreferences columnPreferences;
    private final BibDatabaseContext database;
    private final CellFactory cellFactory;
    private final UndoManager undoManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final StateManager stateManager;
    private final MainTableTooltip tooltip;

    public MainTableColumnFactory(BibDatabaseContext database,
                                  GuiPreferences preferences,
                                  ColumnPreferences abstractColumnPrefs,
                                  UndoManager undoManager,
                                  DialogService dialogService,
                                  StateManager stateManager,
                                  TaskExecutor taskExecutor) {
        this.database = Objects.requireNonNull(database);
        this.preferences = Objects.requireNonNull(preferences);
        this.columnPreferences = abstractColumnPrefs;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.cellFactory = new CellFactory(preferences, undoManager);
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        ThemeManager themeManager = Injector.instantiateModelOrService(ThemeManager.class);
        this.tooltip = new MainTableTooltip(dialogService, preferences, themeManager, taskExecutor);
    }

    public TableColumn<BibEntryTableViewModel, ?> createColumn(MainTableColumnModel column) {
        TableColumn<BibEntryTableViewModel, ?> returnColumn = null;
        switch (column.getType()) {
            case INDEX:
                returnColumn = createIndexColumn(column);
                break;
            case GROUPS:
                returnColumn = createGroupColumn(column);
                break;
            case GROUP_ICONS:
                returnColumn = createGroupIconColumn(column);
                break;
            case FILES:
                returnColumn = createFilesColumn(column);
                break;
            case LINKED_IDENTIFIER:
                returnColumn = createIdentifierColumn(column);
                break;
            case LIBRARY_NAME:
                returnColumn = createLibraryColumn(column);
                break;
            case EXTRAFILE:
                if (!column.getQualifier().isBlank()) {
                    returnColumn = createExtraFileColumn(column);
                }
                break;
            case SPECIALFIELD:
                if (!column.getQualifier().isBlank()) {
                    Field field = FieldFactory.parseField(column.getQualifier());
                    if (field instanceof SpecialField) {
                        returnColumn = createSpecialFieldColumn(column);
                    } else {
                        LOGGER.warn("Special field type '{}' is unknown. Using normal column type.", column.getQualifier());
                        returnColumn = createFieldColumn(column, tooltip);
                    }
                }
                break;
            case NORMALFIELD:
                if (!column.getQualifier().isBlank()) {
                    returnColumn = createFieldColumn(column, tooltip);
                }
                break;
            default:
        }
        return returnColumn;
    }

    public List<TableColumn<BibEntryTableViewModel, ?>> createColumns() {
        List<TableColumn<BibEntryTableViewModel, ?>> columns = new ArrayList<>();

        columns.add(createMatchCategoryColumn(new MainTableColumnModel(MainTableColumnModel.Type.MATCH_CATEGORY)));
        columnPreferences.getColumns().forEach(column -> columns.add(createColumn(column)));
        return columns;
    }

    public static void setExactWidth(TableColumn<?, ?> column, double width) {
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * Creates a column for the match category.
     * <p>This column is always hidden but is used for sorting the table
     * in the floating mode. The order of the {@link MatchCategory} enum constants
     * determines the sorting order.</p>
     */
    private TableColumn<BibEntryTableViewModel, MatchCategory> createMatchCategoryColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, MatchCategory> column = new MainTableColumn<>(columnModel);
        column.setCellValueFactory(cellData -> cellData.getValue().matchCategory());
        column.setSortable(true);
        column.setSortType(TableColumn.SortType.ASCENDING);
        column.setVisible(false);
        return column;
    }

    /**
     * Creates a column with a continuous number
     */
    private TableColumn<BibEntryTableViewModel, String> createIndexColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, String> column = new MainTableColumn<>(columnModel);
        Node header = new Text("#");
        header.getStyleClass().add("mainTable-header");
        Tooltip.install(header, new Tooltip(MainTableColumnModel.Type.INDEX.getDisplayName()));
        column.setGraphic(header);
        column.setStyle("-fx-alignment: CENTER-RIGHT;");
        column.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
                String.valueOf(cellData.getTableView().getItems().indexOf(cellData.getValue()) + 1)));
        new ValueTableCellFactory<BibEntryTableViewModel, String>()
                .withText(text -> text)
                .install(column);
        column.setSortable(false);
        return column;
    }

    /**
     * Creates a column for group color bars.
     */
    private TableColumn<BibEntryTableViewModel, ?> createGroupColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, List<AbstractGroup>> column = new MainTableColumn<>(columnModel);
        Node headerGraphic = IconTheme.JabRefIcons.DEFAULT_GROUP_ICON.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Group color")));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(STYLE_ICON_COLUMN);
        setExactWidth(column, ColumnPreferences.ICON_COLUMN_WIDTH);
        column.setResizable(false);
        column.setCellValueFactory(cellData -> cellData.getValue().getMatchedGroups());
        new ValueTableCellFactory<BibEntryTableViewModel, List<AbstractGroup>>()
                .withGraphic(this::createGroupColorRegion)
                .install(column);
        column.setStyle("-fx-padding: 0 0 0 0;");
        column.setSortable(true);
        return column;
    }

    /**
     * Creates a column for group icons
     */
    private TableColumn<BibEntryTableViewModel, ?> createGroupIconColumn(MainTableColumnModel columnModel) {
        TableColumn<BibEntryTableViewModel, List<AbstractGroup>> column = new MainTableColumn<>(columnModel);
        Node headerGraphic = IconTheme.JabRefIcons.DEFAULT_GROUP_ICON_COLUMN.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(MainTableColumnModel.Type.GROUP_ICONS.getDisplayName()));
        column.setGraphic(headerGraphic);
        column.getStyleClass().add(STYLE_ICON_COLUMN);
        column.setResizable(true);
        column.setCellValueFactory(cellData -> cellData.getValue().getMatchedGroups());
        new ValueTableCellFactory<BibEntryTableViewModel, List<AbstractGroup>>()
                .withGraphic(this::createGroupIconRegion)
                .install(column);
        column.setStyle("-fx-padding: 0 0 0 0;");
        column.setSortable(true);
        return column;
    }

    private Node createGroupColorRegion(BibEntryTableViewModel entry, List<AbstractGroup> matchedGroups) {
        List<Color> groupColors = matchedGroups.stream()
                                               .flatMap(group -> group.getColor().stream())
                                               .toList();

        if (!groupColors.isEmpty()) {
            HBox container = new HBox();
            container.setSpacing(2);
            container.setMinWidth(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(0, 2, 0, 2));

            groupColors.stream().distinct().forEach(groupColor -> {
                Rectangle groupRectangle = new Rectangle();
                groupRectangle.getStyleClass().add("groupColumnBackground");
                groupRectangle.setWidth(3);
                groupRectangle.setHeight(18);
                groupRectangle.setFill(groupColor);
                groupRectangle.setStrokeWidth(1);
                container.getChildren().add(groupRectangle);
            });

            String matchedGroupsString = matchedGroups.stream()
                                                      .distinct()
                                                      .map(AbstractGroup::getName)
                                                      .collect(Collectors.joining(", "));
            Tooltip tooltip = new Tooltip(Localization.lang("Entry is contained in the following groups:") + "\n" + matchedGroupsString);
            Tooltip.install(container, tooltip);
            return container;
        }
        return new Pane();
    }

    private Node createGroupIconRegion(BibEntryTableViewModel entry, List<AbstractGroup> matchedGroups) {
        List<JabRefIcon> groupIcons = matchedGroups.stream()
                                                   .filter(abstractGroup -> abstractGroup.getIconName().isPresent())
                                                   .flatMap(group -> IconTheme.findIcon(group.getIconName().get(), group.getColor().orElse(IconTheme.getDefaultGroupColor())).stream()
                                                   )
                                                   .toList();
        if (!groupIcons.isEmpty()) {
            HBox container = new HBox();
            container.setSpacing(2);
            container.setMinWidth(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(0, 2, 0, 2));

            groupIcons.stream().distinct().forEach(groupIcon -> container.getChildren().add(groupIcon.getGraphicNode()));

            String matchedGroupsString = matchedGroups.stream()
                                                      .distinct()
                                                      .map(AbstractGroup::getName)
                                                      .collect(Collectors.joining(", "));
            Tooltip tooltip = new Tooltip(Localization.lang("Entry is contained in the following groups:") + "\n" + matchedGroupsString);
            Tooltip.install(container, tooltip);
            return container;
        }
        return new Pane();
    }

    /**
     * Creates a text column to display any standard field.
     */
    private TableColumn<BibEntryTableViewModel, ?> createFieldColumn(MainTableColumnModel columnModel, MainTableTooltip tooltip) {
        return new FieldColumn(columnModel, tooltip);
    }

    /**
     * Creates a clickable icons column for DOIs, URLs, URIs and EPrints.
     */
    private TableColumn<BibEntryTableViewModel, Map<Field, String>> createIdentifierColumn(MainTableColumnModel columnModel) {
        return new LinkedIdentifierColumn(columnModel, cellFactory, database, dialogService, preferences, stateManager);
    }

    /**
     * Creates a column that displays a {@link SpecialField}
     */
    private TableColumn<BibEntryTableViewModel, Optional<SpecialFieldValueViewModel>> createSpecialFieldColumn(MainTableColumnModel columnModel) {
        return new SpecialFieldColumn(columnModel, preferences, undoManager);
    }

    /**
     * Creates a column for all the linked files. Instead of creating a column for a single file type, like {@link
     * #createExtraFileColumn(MainTableColumnModel)} createExtraFileColumn} does, this creates one single column collecting all file links.
     */
    private TableColumn<BibEntryTableViewModel, List<LinkedFile>> createFilesColumn(MainTableColumnModel columnModel) {
        return new FileColumn(columnModel,
                database,
                dialogService,
                preferences,
                taskExecutor);
    }

    /**
     * Creates a column for all the linked files of a single file type.
     */
    private TableColumn<BibEntryTableViewModel, List<LinkedFile>> createExtraFileColumn(MainTableColumnModel columnModel) {
        return new FileColumn(columnModel,
                database,
                dialogService,
                preferences,
                columnModel.getQualifier(),
                taskExecutor);
    }

    /**
     * Create library column containing the Filename of the library's bib file
     */
    private TableColumn<BibEntryTableViewModel, String> createLibraryColumn(MainTableColumnModel columnModel) {
        return new LibraryColumn(columnModel);
    }
}
