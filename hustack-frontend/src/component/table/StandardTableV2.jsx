import { makeStyles, MuiThemeProvider } from "@material-ui/core/styles";
import FirstPageIcon from "@mui/icons-material/FirstPage";
import KeyboardArrowLeft from "@mui/icons-material/KeyboardArrowLeft";
import KeyboardArrowRight from "@mui/icons-material/KeyboardArrowRight";
import LastPageIcon from "@mui/icons-material/LastPage";
import { Box, IconButton, Paper, Typography } from "@mui/material";
import MaterialTable, { MTableCell, MTableToolbar } from "material-table";
import PropTypes from "prop-types";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { components, localization, tableIcons, themeTable } from "utils/MaterialTableUtils";

export function TablePaginationActions(props) {
  const { count, page, rowsPerPage, onPageChange } = props;

  const handleFirstPageButtonClick = (event) => {
    onPageChange(event, 0);
  };

  const handleBackButtonClick = (event) => {
    onPageChange(event, page - 1);
  };

  const handleNextButtonClick = (event) => {
    onPageChange(event, page + 1);
  };

  const handleLastPageButtonClick = (event) => {
    onPageChange(event, Math.max(0, Math.ceil(count / rowsPerPage) - 1));
  };

  return (
    <Box sx={{ flexShrink: 0, ml: 2.5 }}>
      <IconButton
        onClick={handleFirstPageButtonClick}
        disabled={page === 0}
        aria-label="first page"
      >
        <FirstPageIcon />
      </IconButton>
      <IconButton
        onClick={handleBackButtonClick}
        disabled={page === 0}
        aria-label="previous page"
      >
        <KeyboardArrowLeft />
      </IconButton>
      <IconButton
        onClick={handleNextButtonClick}
        disabled={page >= Math.ceil(count / rowsPerPage) - 1}
        aria-label="next page"
      >
        <KeyboardArrowRight />
      </IconButton>
      <IconButton
        onClick={handleLastPageButtonClick}
        disabled={page >= Math.ceil(count / rowsPerPage) - 1}
        aria-label="last page"
      >
        <LastPageIcon />
      </IconButton>
    </Box>
  );
}

const useStyles = makeStyles(() => ({
  tableToolbarHighlight: { backgroundColor: "transparent" },
}));

export default function StandardTable(props) {
  const classes = useStyles();
  const { t } = useTranslation(["common"]);

  const rowStyle = useCallback(
    (rowData) => ({
      backgroundColor: rowData.tableData.checked ? "#e0e0e0" : "#ffffff",
    }),
    []
  );

  const getPointsColor = () => {
    const submitted = props.totalSubmittedPoints || 0;
    const max = props.totalMaxPoints || 0;
    if (submitted === 0) return "#f44336"; 
    if (submitted < max) return "#0288d1"; 
    return "#4caf50";
  };

  return (
    <>
      {!props.hideCommandBar && (
        <Box
          sx={{
            width: "100%",
            height: 40,
            display: "flex",
            justifyContent: "flex-start",
            alignItems: "center",
            borderBottom: "1px solid rgb(224, 224, 224)",
            pl: 2,
            backgroundColor: "#f5f5f5",
            ...props.sx?.commandBar,
          }}
        >
          {props.commandBarComponents}
        </Box>
      )}
      <MuiThemeProvider theme={themeTable}>
        <MaterialTable
          {...props}
          title={
            props.title ? (
              <Typography variant="h5">{props.title}</Typography>
            ) : (
              <></>
            )
          }
          localization={{
            ...localization,
            toolbar: {
              searchPlaceholder: t("search"),
            },
            ...props.localization,
          }}
          icons={tableIcons}
          options={{
            selection: true,
            pageSize: 20,
            headerStyle: {
              backgroundColor: "#f4f4f4",
              color: "#404040",
              fontWeight: 600,
              padding: 12,
            },
            rowStyle: rowStyle,
            searchFieldVariant: "outlined",
            ...props.options,
          }}
          onSelectionChange={(rows) => {
            props.onSelectionChange?.(rows);
          }}
          onRowClick={props.onRowClick}
          components={{
            ...components,
            Container: (props) => <Paper {...props} elevation={2} />,
            Toolbar: (toolBarProps) => (
              props.hideToolBar ? null : (
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "8px 16px",
                  }}
                >
                  <Typography
                    variant="h6"
                    sx={{
                      color: getPointsColor(),
                    }}
                  >
                    {t("common:maxSubmittedPoint")}:{" "} 
                    {props.totalSubmittedPoints?.toFixed(2) || 0} /{" "}
                    {t("common:maxPoint")}:{" "}
                    {props.totalMaxPoints?.toFixed(2) || 0}
                  </Typography>
                  <MTableToolbar
                    {...toolBarProps}
                    classes={{
                      highlight: classes.tableToolbarHighlight,
                    }}
                    searchFieldStyle={{
                      height: 40,
                      ...props.options?.searchFieldStyle,
                    }}
                  />
                </Box>
              )
            ),
            Cell: (props) => <MTableCell {...props} style={{ padding: "12px" }} />,
            ...props.components,
          }}
          actions={props.actions}
          editable={props.editable}
        />
      </MuiThemeProvider>
    </>
  );
}

StandardTable.propTypes = {
  hideCommandBar: PropTypes.bool,
  hideToolBar: PropTypes.bool,
  classNames: PropTypes.object,
  localization: PropTypes.object,
  options: PropTypes.object,
  onSelectionChange: PropTypes.func,
  onRowClick: PropTypes.func,
  components: PropTypes.object,
  title: PropTypes.string,
  columns: PropTypes.array.isRequired,
  actions: PropTypes.array,
  data: PropTypes.oneOfType([PropTypes.array, PropTypes.func]),
  commandBarComponents: PropTypes.element,
  editable: PropTypes.object,
  totalSubmittedPoints: PropTypes.number, 
  totalMaxPoints: PropTypes.number, 
};