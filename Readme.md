1. To get all employees:
GET - http://localhost:8080/api/employe

2. To get employee by id:
GET - http://localhost:8080/api/employe/2

3. To add an employee:
POST - http://localhost:8080/api/employee
Body - {
            "name":"Jhon Do",
            "role":"emp",
            "absent":0
        }

4. To edit an employee: (edit it)
PUT - http://localhost:8080/api/employee
Body - {
            "name":"Jhon Do2"
        }

5. To delete an employee:
DELETE - http://localhost:8080/api/employee/3

6. To mark attendance for single student
PUT - http://localhost:8080/api/attendance/mark
Body - {
            "employeeId": 2,
            "absent": true,
            "date":
        }

7. To mark attendance for entire batch
POST - http://localhost:8080/api/attendance/mark-batch
Body - [
        {
            "employeeId": 1,
            "absent": false,
            "date": "2025-11-27"
        },
        {
            "employeeId": 2,
            "absent": true
        },
        {
            "employeeId": 3,
            "absent": false
        }
    ]

