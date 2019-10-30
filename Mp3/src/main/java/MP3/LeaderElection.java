class LeaderElection extends Thread
{
    private static GrepLogger logger = GrepLogger.getInstance();
    
    @Override
    public void run()
    {
        String newLeaderIpAddress = MembershipList.selectNewLeader();
        List<String> higherIpAddress = MembershipList.getHigherNodesIpAdress();
    }
}