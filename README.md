# lending-platform
Lending platform

docker run --name postgres -e POSTGRES_PASSWORD=apis -e POSTGRES_USER=apis -e POSTGRES_MULTIPLE_DATABASES=ecommerce,products,loans,crm,notification -p 5432:5432 -d postgres



Service,Primary Entities,Key Responsibility
Orchestrator,"SagaInstance, SagaStepLog","Tracks the state of global transactions (e.g., STARTED, COMPENSATING, COMPLETED)."
Product-Config,"LoanProduct, InterestRule","Single source of truth for loan terms, APR, and regional constraints."
Loan-Disbursement,"Disbursement, PayoutLog",Manages the actual transfer of money to the borrower’s account.
Loan-Repayment,"PaymentSchedule, Transaction","Tracks installments, balances, and incoming payments."
Scheduler,"ScheduledJob, Trigger","Manages time-based triggers (e.g., ""Run daily interest calculation"")."
Notification,"Template, DispatchLog",Stores message templates and records of sent SMS/Emails.