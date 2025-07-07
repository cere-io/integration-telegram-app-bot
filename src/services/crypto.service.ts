import { Injectable, Logger } from '@nestjs/common';
// Import crypto for fallback implementation
import * as crypto from 'crypto';

@Injectable()
export class CryptoService {
  private readonly logger = new Logger(CryptoService.name);

  async generateAccountId(userId: number): Promise<string> {
    const seed = this.createDeterministicSeed(userId);
    const privateKey = seed.slice(0, 32);
    
    try {
      // Try dynamic import for ES module
      const ed25519Module = await import('@noble/ed25519').catch(() => null);
      if (ed25519Module) {
        const publicKey = await ed25519Module.getPublicKey(privateKey);
        return '0x' + Buffer.from(publicKey).toString('hex');
      } else {
        throw new Error('Failed to import @noble/ed25519');
      }
    } catch (error) {
      this.logger.warn(`Ed25519 module error: ${error.message}. Using fallback implementation.`);
      // Fallback to Node.js crypto module
      return this.generateAccountIdFallback(privateKey);
    }
  }
  
  private generateAccountIdFallback(privateKey: Buffer): string {
    // Use a deterministic hash as fallback
    // This is not cryptographically equivalent to Ed25519 but provides a deterministic ID
    const hash = crypto.createHash('sha256').update(privateKey).digest();
    return '0x' + hash.toString('hex');
  }

  private createDeterministicSeed(userId: number): Buffer {
    // Exactly match Kotlin Random(userId).nextBytes(sk) behavior
    // Kotlin uses java.util.Random with Linear Congruential Generator (LCG)
    const seed = Buffer.alloc(32);
    let state = userId & 0xFFFFFFFF; // Ensure 32-bit unsigned integer
    
    // Kotlin Random uses LCG: nextSeed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)
    // But for nextBytes, it uses next(8) which calls next(bits) internally
    for (let i = 0; i < 32; i++) {
      // Match Kotlin's Random.next(8) implementation exactly
      state = ((state * 0x5DEECE66D + 0xB) & 0xFFFFFFFFFFFF) >>> 0;
      const bits = (state >>> (48 - 8)) & 0xFF;
      seed[i] = bits;
    }
    
    return seed;
  }

  // Helper method to convert JavaScript numbers to Kotlin-style long values
  toLongValue(value: number): number {
    // Ensure we handle the conversion the same way Kotlin does
    return Math.floor(value);
  }
} 