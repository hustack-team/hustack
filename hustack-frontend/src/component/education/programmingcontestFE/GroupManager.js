import React, { useState } from "react";
import { AntTabs, AntTab, TabPanel, a11yProps } from "component/tab";
import withScreenSecurity from "component/withScreenSecurity";
import { useParams } from "react-router-dom";
import GroupManagerDetail from "./GroupManagerDetail";
import GroupManagerMembers from "./GroupManagerMembers";

function GroupManager({ screenAuthorization }) {
  const { groupId } = useParams();
  const [selectedTab, setSelectedTab] = useState(0);
  const [refreshMembers, setRefreshMembers] = useState(false);

  const handleChange = (event, newValue) => {
    setSelectedTab(newValue);
  };

  return (
    <>
      <AntTabs
        value={selectedTab}
        onChange={handleChange}
        aria-label="group manager tabs"
        scrollButtons="auto"
        variant="scrollable"
      >
        <AntTab label="General" {...a11yProps(0)} />
        <AntTab label="Members" {...a11yProps(1)} />
      </AntTabs>

      <TabPanel value={selectedTab} index={0} dir={"ltr"}>
        <GroupManagerDetail groupId={groupId} />
      </TabPanel>

      <TabPanel value={selectedTab} index={1} dir={"ltr"}>
        <GroupManagerMembers
          groupId={groupId}
          screenAuthorization={screenAuthorization}
          refresh={refreshMembers}
          setRefreshMembers={setRefreshMembers}
        />
      </TabPanel>
    </>
  );
}

const screenName = "SCR_GROUP_MANAGER";
export default withScreenSecurity(GroupManager, screenName, true);