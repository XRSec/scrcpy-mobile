//
//  ViewController.m
//  adb-demo
//
//  Created by Ethan on 2022/6/1.
//

#import "ViewController.h"
#import "adb_public.h"

void adb_connect_status_updated(const char *serial, const char *status) {
    NSLog(@"ADB Status: %s, %s", serial, status);
}

@interface ViewController ()
@property (weak, nonatomic) IBOutlet UITextField *adbHost;
@property (weak, nonatomic) IBOutlet UIButton *adbConnect;
@property (weak, nonatomic) IBOutlet UITextField *adbCommand;
@property (weak, nonatomic) IBOutlet UIButton *adbExecute;

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    adb_enable_trace();
    adb_set_server_port("15037");
    
    NSArray <NSString *> *documentPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    const char *document_home = documentPaths.lastObject.UTF8String;
    adb_set_home(document_home);
    
    // Load defaults
    NSString *host = [NSUserDefaults.standardUserDefaults objectForKey:@"adbHost"];
    if (host.length > 0) {
        self.adbHost.text = host;
    }
    NSString *command = [NSUserDefaults.standardUserDefaults objectForKey:@"adbCommand"];
    if (command.length > 0) {
        self.adbCommand.text = command;
    }
    
    // Disable auto-correction
    self.adbHost.autocorrectionType = UITextAutocorrectionTypeNo;
    self.adbCommand.autocorrectionType = UITextAutocorrectionTypeNo;
}

-(NSString *)adbExecute:(NSArray <NSString *> *)commands success:(BOOL*)success {
    const char *argv[commands.count];
    for (NSInteger i = 0; i < commands.count; i++) {
        argv[i] = commands[i].UTF8String;
    }
    
    char *output = NULL;
    size_t output_size = 0;
    int ret = adb_commandline_porting(&output, &output_size, (int)commands.count, argv);
    *success = ret == 0;
    
    if (output_size > 0) {
        return [NSString stringWithUTF8String:output];
    }
    
    return @"";
}

-(void)showAlert:(NSString *)message {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Message" message:message preferredStyle:(UIAlertControllerStyleAlert)];
    [alertController addAction:[UIAlertAction actionWithTitle:@"OK" style:(UIAlertActionStyleCancel) handler:nil]];
    [self presentViewController:alertController animated:YES completion:nil];
}

- (IBAction)connect:(id)sender {
    if (self.adbHost.text.length == 0) {
        [self showAlert:@"Host is empty!"];
        return;
    }
    
    [self.adbHost endEditing:YES];
    
    // Save text value into user defaults
    [NSUserDefaults.standardUserDefaults setObject:self.adbHost.text forKey:@"adbHost"];
    [NSUserDefaults.standardUserDefaults synchronize];
    
    NSDate *timeStart = [NSDate date];
    
    BOOL success = NO;
    NSString *message = [self adbExecute:@[ @"connect", self.adbHost.text ] success:&success];
    NSLog(@"Time cost: %0.4fs", -[timeStart timeIntervalSinceNow]);
    [self showAlert:[NSString stringWithFormat:@"Success: %@\nMessage: %@", success?@"YES":@"NO", message]];
}

- (IBAction)execute:(id)sender {
    if (self.adbCommand.text.length == 0) {
        [self showAlert:@"Command is empty!"];
        return;
    }
    
    [self.adbCommand endEditing:YES];
    
    // Save text value into user defaults
    [[NSUserDefaults standardUserDefaults] setObject:self.adbCommand.text forKey:@"adbCommand"];
    [[NSUserDefaults standardUserDefaults] synchronize];
    
    BOOL success = NO;
    NSString *message = [self adbExecute:[self.adbCommand.text componentsSeparatedByString:@" "] success:&success];
    [self showAlert:[NSString stringWithFormat:@"Success: %@\nMessage: %@", success?@"YES":@"NO", message]];
}

@end
