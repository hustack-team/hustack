import withScreenSecurity from "component/withScreenSecurity";
import {Paper} from "@mui/material";
import { ListTeacherGroupByMembership } from "./ListTeacherGroupByMembership";

function ListContestManager() {
  return (
    <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}}>
      <ListTeacherGroupByMembership/>
      {/*<ListContestAll/>*/}
    </Paper>
  );
}

const screenName = "SCR_MANAGER_CONTEST_LIST";
export default withScreenSecurity(ListContestManager, screenName, true);
