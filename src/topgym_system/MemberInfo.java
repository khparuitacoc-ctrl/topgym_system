/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;

/**
 *
 * @author Khastrel Troy Ruita
 */
public class MemberInfo {
    private final String firstName;
    private final String lastName;
    private final String age;
    private final String address;
    private final String contactNumber;
    private String gender;

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public MemberInfo(String firstName, String lastName, String age, String address, String contactNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getAge() { return age; }
    public String getAddress() { return address; }
    public String getContactNumber() { return contactNumber; }

    public String getFullName() { return firstName + " " + lastName; }
private java.time.LocalDate endDate;

    public java.time.LocalDate getEndDate() { return endDate; }

    public void setEndDate(java.time.LocalDate endDate) { this.endDate = endDate;}
    
    private int id;
    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public long getDaysLeft() {
        if (endDate == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), endDate);
        
    }

    public boolean isExpired() {
        return endDate != null && getDaysLeft() < 0;
    }

    public String getStatusText() {
        if (endDate == null) return "No expiry date";
        long days = getDaysLeft();
        if (days < 0)  return "Expired";
        if (days == 0) return "Expires today";
        if (days <= 7) return "Expires in " + days + (days == 1 ? " day" : " days");
        return "Active until " + endDate.format(
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH));
    }

    public java.awt.Color getStatusColor() {
        if (endDate == null) return java.awt.Color.GRAY;
        long days = getDaysLeft();
        if (days < 0)  return new java.awt.Color(255, 77, 77);   // red
        if (days <= 7) return new java.awt.Color(255, 165, 0);   // orange
        return new java.awt.Color(76, 175, 80);                  // green
        
    }
}
