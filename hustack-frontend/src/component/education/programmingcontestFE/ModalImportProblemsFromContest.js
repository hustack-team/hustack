import {LoadingButton} from "@mui/lab";
import {Box, TextField} from "@mui/material";
import {request} from "api";
import HustModal from "component/common/HustModal";
import React, {useState} from "react";
import {errorNoti} from "utils/notification";
import StandardTable from "../../table/StandardTable";
import {useTranslation} from "react-i18next";

const ModalImportProblemsFromContest = (props) => {
  const { contestId, isOpen, handleClose } = props;
  const { t } = useTranslation();

  const [fromContestId, setFromContestId] = useState("");
  const [importedProblems, setImportedProblems] = useState([]);
  const [loading, setLoading] = useState(false);

  const isValidContestId = () => {
    return new RegExp(/[%^/\\|.?;[\]]/g).test(fromContestId);
  };

  const handleImportProblems = () => {
    setLoading(true);
    let body = {
      contestId: contestId,
      fromContestId: fromContestId,
    };

    request(
      "post",
      "/contests/import-problems",
      (res) => {
        setImportedProblems(res.data);
      },
      {
        onError: (err) => {
          errorNoti(t("common:error"), 5000);
        },
      },
      body
    )
      .then()
      .finally(() => setLoading(false));
  };

  const getUploadStatusColor = (status) => {
    if (status === "SUCCESSFUL") return "green";
    return "red";
  };

  const columns = [
    { title: "Problem ID", field: "problemId" },
    {
      title: "Status",
      field: "status",
      render: (rowData) => (
        <span style={{ color: getUploadStatusColor(`${rowData.status}`) }}>
          {`${rowData.status}`}
        </span>
      ),
    },
  ];

  return (
    <HustModal
      title={t("common:importProblemsFromContest")}
      open={isOpen}
      onClose={handleClose}
      isLoading={loading}
      isNotShowCloseButton
      maxWidthPaper={importedProblems.length > 0 ? 680 : 480}
    >
      <Box
        display="flex"
        alignItems="center"
        justifyContent="space-between"
        sx={{ mb: 2 }}
      >
        <TextField
          autoFocus
          required
          size="small"
          value={fromContestId}
          id="importFromContestId"
          label={t("common:contestId")}
          onChange={(event) => {
            setFromContestId(event.target.value);
          }}
          error={isValidContestId()}
          helperText={isValidContestId() ? t("common:invalidContestId") : ""}
          sx={{ width: "75%", mr: 2 }}
        />
        <LoadingButton
          // sx={{ textTransform: "none" }}
          loading={loading}
          variant="contained"
          onClick={handleImportProblems}
          disabled={isValidContestId()}
        >
          {t("common:import")}
        </LoadingButton>
      </Box>
      {importedProblems.length > 0 && (
        <StandardTable
          title={
            `${importedProblems.filter((problem) => problem.status === "SUCCESSFUL").length}/${importedProblems.length} ${t("common:successful")}`
          }
          columns={columns}
          data={importedProblems}
          options={{
            selection: false,
            pageSize: 5,
            search: true,
            sorting: true,
          }}
          hideCommandBar
        />
      )}
    </HustModal>
  );
};

export default React.memo(ModalImportProblemsFromContest);
