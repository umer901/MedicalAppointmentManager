package smm.model;
public class UserProfile {
    public String name = "";
    public String email = "";
    public String phoneNumber = "";
    public InsuranceLevel insurance = InsuranceLevel.PREMIUM;
    public boolean notifEmail = true, notifSMS = false, notifInApp = true;
    public boolean twoFA = false;
}
